package com.example.agent.pipeline.step;

import com.example.agent.core.dto.AgentContext;
import com.example.agent.core.dto.KnowledgeChunk;
import com.example.agent.core.dto.PlanDetail;
import com.example.agent.core.strategy.KnowledgeSourceStrategy;
import com.example.agent.knowledge.factory.KnowledgeSourceFactory;
import com.example.agent.pipeline.template.AbstractPipelineStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * 知识召回步骤
 * 负责根据查询规划调用相应的知识源策略，检索相关知识
 * 
 * @author agent
 */
@Component
public class KnowledgeRecallStep extends AbstractPipelineStep {
    
    private static final String STEP_NAME = "KNOWLEDGE_RECALL";
    
    @Autowired
    private KnowledgeSourceFactory knowledgeSourceFactory;
    
    @Override
    protected boolean doExecute(AgentContext context) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 验证必要的上下文信息
            if (!validateContext(context, "query")) {
                return false;
            }
            
            // 获取计划详情
            PlanDetail planDetail = getPlanDetail(context);
            if (planDetail == null) {
                logger.error("未找到计划详情，无法执行知识召回");
                return false;
            }
            
            // 执行知识检索
            List<KnowledgeChunk> knowledgeChunks = retrieveKnowledge(context, planDetail);
            
            // 后处理知识块
            List<KnowledgeChunk> processedChunks = postProcessKnowledge(context, knowledgeChunks);
            
            // 保存检索结果到上下文
            context.addExtensionProperty("knowledgeChunks", processedChunks);
            context.addExtensionProperty("knowledgeCount", processedChunks.size());
            
            // 记录执行时间
            long duration = System.currentTimeMillis() - startTime;
            recordExecutionTime(context, STEP_NAME, duration);
            
            logger.info("知识召回完成 - 查询ID: {}, 检索到 {} 个知识块", 
                    context.getQueryId(), processedChunks.size());
            
            return true;
            
        } catch (Exception e) {
            logger.error("知识召回步骤执行失败", e);
            return false;
        }
    }
    
    /**
     * 获取计划详情
     */
    private PlanDetail getPlanDetail(AgentContext context) {
        Object planDetailObj = context.getExtensionProperty("planDetail");
        if (planDetailObj instanceof PlanDetail) {
            return (PlanDetail) planDetailObj;
        }
        return null;
    }
    
    /**
     * 检索知识
     */
    private List<KnowledgeChunk> retrieveKnowledge(AgentContext context, PlanDetail planDetail) {
        List<String> strategies = planDetail.getKnowledgeSourceStrategies();
        if (strategies == null || strategies.isEmpty()) {
            logger.warn("未配置知识源策略，使用默认策略");
            strategies = Arrays.asList("VECTOR_STORE");
        }
        
        // 并行执行多个策略
        List<CompletableFuture<List<KnowledgeChunk>>> futures = strategies.stream()
                .map(strategyName -> CompletableFuture.supplyAsync(() -> 
                        retrieveFromStrategy(context, strategyName)))
                .collect(Collectors.toList());
        
        // 等待所有策略完成
        List<KnowledgeChunk> allChunks = new ArrayList<>();
        for (CompletableFuture<List<KnowledgeChunk>> future : futures) {
            try {
                List<KnowledgeChunk> chunks = future.get();
                if (chunks != null) {
                    allChunks.addAll(chunks);
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.error("知识检索异步执行失败", e);
            }
        }
        
        return allChunks;
    }
    
    /**
     * 从特定策略检索知识
     */
    private List<KnowledgeChunk> retrieveFromStrategy(AgentContext context, String strategyName) {
        try {
            logger.debug("执行知识检索策略: {}", strategyName);
            
            KnowledgeSourceStrategy strategy = knowledgeSourceFactory.getStrategy(strategyName);
            if (strategy == null) {
                logger.warn("未找到知识源策略: {}", strategyName);
                return Collections.emptyList();
            }
            
            // 执行检索
            List<KnowledgeChunk> chunks = strategy.retrieve(context);
            
            // 为每个知识块标记来源策略
            if (chunks != null) {
                chunks.forEach(chunk -> {
                    chunk.setSourceType(strategyName);
                    chunk.setRetrievalTime(new Date());
                });
            }
            
            logger.debug("策略 {} 检索到 {} 个知识块", strategyName, chunks != null ? chunks.size() : 0);
            return chunks != null ? chunks : Collections.emptyList();
            
        } catch (Exception e) {
            logger.error("策略 {} 执行失败", strategyName, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 后处理知识块
     */
    private List<KnowledgeChunk> postProcessKnowledge(AgentContext context, List<KnowledgeChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 去重
        List<KnowledgeChunk> deduplicatedChunks = deduplicateChunks(chunks);
        
        // 排序
        List<KnowledgeChunk> sortedChunks = sortByRelevance(deduplicatedChunks);
        
        // 截取最相关的结果
        int maxResults = getMaxResults(context);
        if (sortedChunks.size() > maxResults) {
            sortedChunks = sortedChunks.subList(0, maxResults);
        }
        
        // 过滤低质量结果
        List<KnowledgeChunk> filteredChunks = filterLowQuality(sortedChunks, context);
        
        logger.debug("知识后处理完成 - 原始: {}, 去重: {}, 排序截取: {}, 过滤后: {}", 
                chunks.size(), deduplicatedChunks.size(), sortedChunks.size(), filteredChunks.size());
        
        return filteredChunks;
    }
    
    /**
     * 去重知识块
     */
    private List<KnowledgeChunk> deduplicateChunks(List<KnowledgeChunk> chunks) {
        Map<String, KnowledgeChunk> uniqueChunks = new LinkedHashMap<>();
        
        for (KnowledgeChunk chunk : chunks) {
            String key = generateChunkKey(chunk);
            if (!uniqueChunks.containsKey(key)) {
                uniqueChunks.put(key, chunk);
            } else {
                // 如果已存在，选择得分更高的
                KnowledgeChunk existing = uniqueChunks.get(key);
                if (chunk.getScore() > existing.getScore()) {
                    uniqueChunks.put(key, chunk);
                }
            }
        }
        
        return new ArrayList<>(uniqueChunks.values());
    }
    
    /**
     * 生成知识块唯一键
     */
    private String generateChunkKey(KnowledgeChunk chunk) {
        // 使用内容的前100个字符作为键
        String content = chunk.getContent();
        if (content != null && content.length() > 100) {
            content = content.substring(0, 100);
        }
        return content != null ? content.trim() : "";
    }
    
    /**
     * 按相关性排序
     */
    private List<KnowledgeChunk> sortByRelevance(List<KnowledgeChunk> chunks) {
        return chunks.stream()
                .sorted((chunk1, chunk2) -> Double.compare(chunk2.getScore(), chunk1.getScore()))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取最大结果数
     */
    private int getMaxResults(AgentContext context) {
        Object maxResultsObj = getConfigParameter(context, "maxResults", 10);
        if (maxResultsObj instanceof Integer) {
            return (Integer) maxResultsObj;
        }
        return 10;
    }
    
    /**
     * 过滤低质量结果
     */
    private List<KnowledgeChunk> filterLowQuality(List<KnowledgeChunk> chunks, AgentContext context) {
        double threshold = getQualityThreshold(context);
        
        return chunks.stream()
                .filter(chunk -> chunk.getScore() >= threshold)
                .filter(chunk -> chunk.getContent() != null && chunk.getContent().length() > 10)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取质量阈值
     */
    private double getQualityThreshold(AgentContext context) {
        Object thresholdObj = getConfigParameter(context, "similarityThreshold", 0.7);
        if (thresholdObj instanceof Double) {
            return (Double) thresholdObj;
        } else if (thresholdObj instanceof Number) {
            return ((Number) thresholdObj).doubleValue();
        }
        return 0.7;
    }
    
    @Override
    public String getStepName() {
        return STEP_NAME;
    }
    
    @Override
    public boolean checkPreconditions(AgentContext context) {
        // 检查是否有计划详情
        PlanDetail planDetail = getPlanDetail(context);
        return planDetail != null && planDetail.getKnowledgeSourceStrategies() != null;
    }
    
    @Override
    public boolean canSkip(AgentContext context) {
        // 检查是否已经有知识块
        Object knowledgeChunks = context.getExtensionProperty("knowledgeChunks");
        return knowledgeChunks != null;
    }
    
    @Override
    public void afterExecution(AgentContext context, boolean success) {
        if (success) {
            context.setProcessingStatus("KNOWLEDGE_RETRIEVED");
        } else {
            context.setProcessingStatus("KNOWLEDGE_RETRIEVAL_FAILED");
        }
    }
} 