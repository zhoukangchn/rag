package com.example.agent.pipeline.service;

import com.example.agent.core.dto.AgentContext;
import com.example.agent.core.step.PipelineStep;
import com.example.agent.pipeline.chain.ChainContext;
import com.example.agent.pipeline.chain.PipelineChain;
import com.example.agent.pipeline.step.*;
import com.example.agent.pipeline.streaming.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent管道服务外观类
 * 负责管道编排和调用逻辑，提供统一的处理入口
 * 
 * @author agent
 */
@Service
public class AgentPipelineService {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentPipelineService.class);
    
    // 步骤组件注入
    @Autowired
    private PlanningStep planningStep;
    
    @Autowired
    private KnowledgeRecallStep knowledgeRecallStep;
    
    @Autowired
    private PromptConstructionStep promptConstructionStep;
    
    @Autowired
    private ModelInvocationStep modelInvocationStep;
    
    // 预定义的处理链
    private Map<String, PipelineChain> predefinedChains;
    
    // 执行统计
    private Map<String, Object> executionStatistics;
    
    @PostConstruct
    public void init() {
        this.predefinedChains = new ConcurrentHashMap<>();
        this.executionStatistics = new ConcurrentHashMap<>();
        
        // 初始化预定义处理链
        initializePredefinedChains();
        
        logger.info("AgentPipelineService 初始化完成，预定义处理链数量: {}", 
                predefinedChains.size());
    }
    
    /**
     * 初始化预定义处理链
     */
    private void initializePredefinedChains() {
        // 标准处理链
        PipelineChain standardChain = new PipelineChain("standard", "标准处理链")
                .addStep(planningStep)
                .addStep(knowledgeRecallStep)
                .addStep(promptConstructionStep)
                .addStep(modelInvocationStep)
                .build();
        predefinedChains.put("standard", standardChain);
        
        // 快速处理链（跳过知识召回）
        PipelineChain quickChain = new PipelineChain("quick", "快速处理链")
                .addStep(planningStep)
                .addStep(promptConstructionStep)
                .addStep(modelInvocationStep)
                .build();
        predefinedChains.put("quick", quickChain);
        
        // 知识增强处理链（额外的知识处理）
        PipelineChain knowledgeEnhancedChain = new PipelineChain("knowledge-enhanced", "知识增强处理链")
                .addStep(planningStep)
                .addStep(knowledgeRecallStep)
                .addStep(promptConstructionStep)
                .addStep(modelInvocationStep)
                .build();
        predefinedChains.put("knowledge-enhanced", knowledgeEnhancedChain);
    }
    
    /**
     * 处理请求（使用标准处理链）
     */
    public AgentProcessingResult process(String query, String userId, String sessionId) {
        return process(query, userId, sessionId, "standard", null);
    }
    
    /**
     * 处理请求（指定处理链类型）
     */
    public AgentProcessingResult process(String query, String userId, String sessionId, 
                                       String chainType) {
        return process(query, userId, sessionId, chainType, null);
    }
    
    /**
     * 处理请求（完整参数）
     */
    public AgentProcessingResult process(String query, String userId, String sessionId, 
                                       String chainType, Map<String, Object> options) {
        long startTime = System.currentTimeMillis();
        String queryId = generateQueryId();
        
        logger.info("开始处理请求 - 查询ID: {}, 用户ID: {}, 会话ID: {}, 处理链: {}", 
                queryId, userId, sessionId, chainType);
        
        try {
            // 创建Agent上下文
            AgentContext agentContext = createAgentContext(query, userId, sessionId, queryId, options);
            
            // 获取处理链
            PipelineChain chain = getOrCreateChain(chainType, agentContext);
            
            // 执行处理链
            ChainContext chainContext = chain.execute(agentContext);
            
            // 构建结果
            AgentProcessingResult result = buildResult(agentContext, chainContext);
            
            // 记录统计信息
            recordStatistics(chainType, chainContext);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("请求处理完成 - 查询ID: {}, 耗时: {}ms, 成功: {}", 
                    queryId, duration, result.isSuccessful());
            
            return result;
            
        } catch (Exception e) {
            logger.error("请求处理异常 - 查询ID: {}", queryId, e);
            
            long duration = System.currentTimeMillis() - startTime;
            return AgentProcessingResult.error(queryId, "处理失败: " + e.getMessage(), duration);
        }
    }
    
    /**
     * 异步处理请求
     */
    public CompletableFuture<AgentProcessingResult> processAsync(String query, String userId, 
                                                               String sessionId, String chainType, 
                                                               Map<String, Object> options) {
        return CompletableFuture.supplyAsync(() -> process(query, userId, sessionId, chainType, options));
    }
    
    /**
     * 异步处理查询（带进度监听器）
     */
    public CompletableFuture<AgentProcessingResult> processQueryAsync(String query, 
                                                                     ProgressListener progressListener) {
        return CompletableFuture.supplyAsync(() -> processWithProgress(query, progressListener));
    }
    
    /**
     * 带进度监听器的处理方法
     */
    private AgentProcessingResult processWithProgress(String query, ProgressListener progressListener) {
        long startTime = System.currentTimeMillis();
        String queryId = generateQueryId();
        
        try {
            progressListener.onStepStarted("initialization");
            
            // 创建Agent上下文
            AgentContext agentContext = createAgentContext(query, "system", "stream", queryId, null);
            
            // 将进度监听器添加到上下文中
            agentContext.addExtensionProperty("progressListener", progressListener);
            agentContext.addExtensionProperty("streamingEnabled", true);
            
            // 获取处理链
            PipelineChain chain = getOrCreateChain("standard", agentContext);
            
            progressListener.onStepCompleted("initialization");
            
            // 执行处理链（带进度监听）
            ChainContext chainContext = executeChainWithProgress(chain, agentContext, progressListener);
            
            // 构建结果
            AgentProcessingResult result = buildResult(agentContext, chainContext);
            
            // 记录统计信息
            recordStatistics("standard", chainContext);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("流式处理完成 - 查询ID: {}, 耗时: {}ms, 成功: {}", 
                    queryId, duration, result.isSuccessful());
            
            return result;
            
        } catch (Exception e) {
            logger.error("流式处理异常 - 查询ID: {}", queryId, e);
            progressListener.onError("处理失败: " + e.getMessage());
            
            long duration = System.currentTimeMillis() - startTime;
            return AgentProcessingResult.error(queryId, "处理失败: " + e.getMessage(), duration);
        }
    }
    
    /**
     * 带进度监听器的处理链执行
     */
    private ChainContext executeChainWithProgress(PipelineChain chain, AgentContext agentContext, 
                                                 ProgressListener progressListener) {
        ChainContext chainContext = new ChainContext(agentContext, chain.getChainId(), chain.getChainName());
        chainContext.start();
        
        List<PipelineStep> steps = chain.getSteps();
        int totalSteps = steps.size();
        boolean allStepsSuccessful = true;
        
        for (int i = 0; i < totalSteps; i++) {
            PipelineStep step = steps.get(i);
            String stepName = step.getStepName();
            
            try {
                progressListener.onStepStarted(stepName);
                
                long stepStartTime = System.currentTimeMillis();
                
                // 执行步骤
                step.execute(agentContext);
                
                long stepDuration = System.currentTimeMillis() - stepStartTime;
                chainContext.recordStepExecution(stepName, stepDuration, true);
                
                progressListener.onStepCompleted(stepName);
                
                // 更新进度
                double progress = ((double) (i + 1) / totalSteps) * 100;
                progressListener.onProgress(stepName, progress, 
                        String.format("完成步骤 %d/%d", i + 1, totalSteps));
                
            } catch (Exception e) {
                logger.error("步骤执行失败: {}", stepName, e);
                progressListener.onStepFailed(stepName, e.getMessage());
                
                // 记录错误状态
                long stepDuration = System.currentTimeMillis() - System.currentTimeMillis();
                chainContext.recordStepExecution(stepName, stepDuration, false);
                chainContext.setException(e);
                
                allStepsSuccessful = false;
                break;
            }
        }
        
        // 完成执行
        chainContext.complete(allStepsSuccessful);
        
        return chainContext;
    }
    
    /**
     * 创建Agent上下文
     */
    private AgentContext createAgentContext(String query, String userId, String sessionId, 
                                          String queryId, Map<String, Object> options) {
        AgentContext context = new AgentContext(sessionId, userId, queryId, query);
        
        // 设置选项参数
        if (options != null) {
            // 用户偏好
            Object userPreferences = options.get("userPreferences");
            if (userPreferences instanceof Map) {
                context.setUserPreferences((Map<String, Object>) userPreferences);
            }
            
            // 检索参数
            Object retrievalParams = options.get("retrievalParams");
            if (retrievalParams instanceof Map) {
                context.setRetrievalParams((Map<String, Object>) retrievalParams);
            }
            
            // 对话历史
            Object conversationHistory = options.get("conversationHistory");
            if (conversationHistory instanceof List) {
                context.setConversationHistory((List<String>) conversationHistory);
            }
            
            // 偏好的知识源
            Object preferredSources = options.get("preferredSources");
            if (preferredSources instanceof List) {
                context.setPreferredSources((List<String>) preferredSources);
            }
            
            // 调试模式
            Object debugMode = options.get("debugMode");
            if (debugMode instanceof Boolean) {
                context.setDebugMode((Boolean) debugMode);
            }
        }
        
        return context;
    }
    
    /**
     * 获取或创建处理链
     */
    private PipelineChain getOrCreateChain(String chainType, AgentContext agentContext) {
        if (chainType == null || chainType.trim().isEmpty()) {
            chainType = "standard";
        }
        
        PipelineChain chain = predefinedChains.get(chainType);
        if (chain != null) {
            return chain;
        }
        
        // 如果没有找到预定义链，根据上下文动态创建
        return createDynamicChain(chainType, agentContext);
    }
    
    /**
     * 创建动态处理链
     */
    private PipelineChain createDynamicChain(String chainType, AgentContext agentContext) {
        logger.info("创建动态处理链 - 类型: {}", chainType);
        
        PipelineChain chain = new PipelineChain(chainType + "_dynamic", "动态" + chainType + "处理链");
        
        // 根据上下文智能选择步骤
        List<PipelineStep> steps = selectStepsBasedOnContext(agentContext);
        
        return chain.addSteps(steps).build();
    }
    
    /**
     * 根据上下文选择步骤
     */
    private List<PipelineStep> selectStepsBasedOnContext(AgentContext agentContext) {
        List<PipelineStep> steps = new ArrayList<>();
        
        // 规划步骤总是需要的
        steps.add(planningStep);
        
        // 根据查询长度和复杂度决定是否需要知识召回
        String query = agentContext.getCurrentQuery();
        if (query != null && (query.length() > 20 || query.contains("?"))) {
            steps.add(knowledgeRecallStep);
        }
        
        // Prompt构建步骤总是需要的
        steps.add(promptConstructionStep);
        
        // 模型调用步骤总是需要的
        steps.add(modelInvocationStep);
        
        return steps;
    }
    
    /**
     * 构建处理结果
     */
    private AgentProcessingResult buildResult(AgentContext agentContext, ChainContext chainContext) {
        AgentProcessingResult result = new AgentProcessingResult();
        
        result.setQueryId(agentContext.getQueryId());
        result.setSuccessful(chainContext.isSuccessful());
        result.setDuration(chainContext.getTotalExecutionTime());
        
        if (chainContext.isSuccessful()) {
            // 获取LLM响应
            Object llmResponse = agentContext.getExtensionProperty("llmResponse");
            result.setResponse(llmResponse != null ? llmResponse.toString() : "");
            
            // 设置额外信息
            result.setKnowledgeChunksCount(getKnowledgeChunksCount(agentContext));
            result.setStepsExecuted(chainContext.getExecutedSteps());
            result.setStepExecutionTimes(chainContext.getStepExecutionTimes());
        } else {
            // 设置错误信息
            String errorMessage = agentContext.getErrorMessage();
            if (errorMessage == null && chainContext.getException() != null) {
                errorMessage = chainContext.getException().getMessage();
            }
            result.setErrorMessage(errorMessage != null ? errorMessage : "未知错误");
        }
        
        // 如果是调试模式，添加调试信息
        if (agentContext.isDebugMode()) {
            result.setDebugInfo(chainContext.getExecutionStatistics());
        }
        
        return result;
    }
    
    /**
     * 获取知识块数量
     */
    private int getKnowledgeChunksCount(AgentContext agentContext) {
        Object knowledgeCount = agentContext.getExtensionProperty("knowledgeCount");
        return knowledgeCount instanceof Integer ? (Integer) knowledgeCount : 0;
    }
    
    /**
     * 记录统计信息
     */
    private void recordStatistics(String chainType, ChainContext chainContext) {
        String key = "chain_" + chainType;
        
        Map<String, Object> stats = (Map<String, Object>) executionStatistics.computeIfAbsent(key, 
                k -> new ConcurrentHashMap<String, Object>());
        
        // 更新计数
        stats.merge("executionCount", 1, (oldVal, newVal) -> (Integer) oldVal + 1);
        stats.merge("successCount", chainContext.isSuccessful() ? 1 : 0, 
                (oldVal, newVal) -> (Integer) oldVal + (Integer) newVal);
        
        // 更新平均执行时间
        long totalTime = (Long) stats.getOrDefault("totalTime", 0L) + chainContext.getTotalExecutionTime();
        stats.put("totalTime", totalTime);
        
        int executionCount = (Integer) stats.get("executionCount");
        stats.put("averageTime", totalTime / executionCount);
    }
    
    /**
     * 生成查询ID
     */
    private String generateQueryId() {
        return "query_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * 获取可用的处理链类型
     */
    public Set<String> getAvailableChainTypes() {
        return new HashSet<>(predefinedChains.keySet());
    }
    
    /**
     * 获取处理链信息
     */
    public Map<String, Object> getChainInfo(String chainType) {
        PipelineChain chain = predefinedChains.get(chainType);
        if (chain == null) {
            return null;
        }
        
        Map<String, Object> info = new HashMap<>();
        info.put("chainId", chain.getChainId());
        info.put("chainName", chain.getChainName());
        info.put("stepCount", chain.getStepCount());
        info.put("stepNames", chain.getStepNames());
        
        return info;
    }
    
    /**
     * 获取执行统计信息
     */
    public Map<String, Object> getExecutionStatistics() {
        return new HashMap<>(executionStatistics);
    }
    
    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        this.executionStatistics.clear();
        logger.info("执行统计信息已重置");
    }
    
} 