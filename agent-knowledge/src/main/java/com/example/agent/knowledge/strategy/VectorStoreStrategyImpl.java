package com.example.agent.knowledge.strategy;

import com.example.agent.core.dto.AgentContext;
import com.example.agent.core.dto.KnowledgeChunk;
import com.example.agent.core.strategy.KnowledgeSourceStrategy;
import com.example.agent.knowledge.repository.VectorStoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 向量数据库检索策略实现
 * 基于向量相似度进行知识检索，支持语义搜索和向量搜索
 * 
 * @author agent
 */
@Component
public class VectorStoreStrategyImpl implements KnowledgeSourceStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(VectorStoreStrategyImpl.class);
    
    private static final String STRATEGY_TYPE = "VECTOR_STORE";
    private static final double DEFAULT_THRESHOLD = 0.7;
    private static final int DEFAULT_LIMIT = 10;
    
    private final VectorStoreRepository vectorStoreRepository;
    
    @Autowired
    public VectorStoreStrategyImpl(VectorStoreRepository vectorStoreRepository) {
        this.vectorStoreRepository = vectorStoreRepository;
    }
    
    @Override
    public String getStrategyType() {
        return STRATEGY_TYPE;
    }
    
    @Override
    public boolean isAvailable() {
        try {
            return vectorStoreRepository.isHealthy();
        } catch (Exception e) {
            logger.warn("Failed to check vector store health: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public List<KnowledgeChunk> retrieveKnowledge(String query, AgentContext context, int limit) {
        if (query == null || query.trim().isEmpty()) {
            logger.warn("Query is null or empty, returning empty results");
            return new ArrayList<>();
        }
        
        try {
            // 从上下文中获取阈值设置，如果没有则使用默认值
            double threshold = getThresholdFromContext(context, DEFAULT_THRESHOLD);
            
            logger.debug("Performing semantic search for query: '{}' with threshold: {} and limit: {}", 
                    query, threshold, limit);
            
            List<KnowledgeChunk> results = vectorStoreRepository.semanticSearch(query, limit, threshold);
            
            // 根据上下文偏好过滤结果
            results = filterByPreferences(results, context);
            
            logger.debug("Vector store search returned {} results", results.size());
            return results;
            
        } catch (Exception e) {
            logger.error("Error during vector store knowledge retrieval: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<KnowledgeChunk> retrieveByKeywords(List<String> keywords, AgentContext context, int limit) {
        if (keywords == null || keywords.isEmpty()) {
            logger.warn("Keywords list is null or empty, returning empty results");
            return new ArrayList<>();
        }
        
        try {
            // 将关键词组合成查询文本
            String combinedQuery = String.join(" ", keywords);
            
            double threshold = getThresholdFromContext(context, DEFAULT_THRESHOLD);
            
            logger.debug("Performing keyword-based semantic search for: '{}' with threshold: {} and limit: {}", 
                    combinedQuery, threshold, limit);
            
            List<KnowledgeChunk> results = vectorStoreRepository.semanticSearch(combinedQuery, limit, threshold);
            
            // 根据关键词匹配度重新排序
            results = reorderByKeywordRelevance(results, keywords);
            
            // 根据上下文偏好过滤结果
            results = filterByPreferences(results, context);
            
            logger.debug("Keyword-based vector search returned {} results", results.size());
            return results;
            
        } catch (Exception e) {
            logger.error("Error during keyword-based vector retrieval: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<KnowledgeChunk> retrieveBySimilarity(List<Double> embedding, AgentContext context, int limit, double threshold) {
        if (embedding == null || embedding.isEmpty()) {
            logger.warn("Embedding vector is null or empty, returning empty results");
            return new ArrayList<>();
        }
        
        try {
            logger.debug("Performing similarity search with embedding vector of size: {} with threshold: {} and limit: {}", 
                    embedding.size(), threshold, limit);
            
            List<KnowledgeChunk> results = vectorStoreRepository.similaritySearch(embedding, limit, threshold);
            
            // 根据上下文偏好过滤结果
            results = filterByPreferences(results, context);
            
            logger.debug("Similarity search returned {} results", results.size());
            return results;
            
        } catch (Exception e) {
            logger.error("Error during similarity-based vector retrieval: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<KnowledgeChunk> retrieveByConditions(Map<String, Object> conditions, AgentContext context, int limit) {
        if (conditions == null || conditions.isEmpty()) {
            logger.warn("Conditions map is null or empty, returning empty results");
            return new ArrayList<>();
        }
        
        try {
            List<KnowledgeChunk> results = new ArrayList<>();
            
            // 处理不同类型的条件
            if (conditions.containsKey("source")) {
                String source = (String) conditions.get("source");
                List<KnowledgeChunk> sourceResults = vectorStoreRepository.findBySource(source);
                results.addAll(sourceResults);
            }
            
            if (conditions.containsKey("tags")) {
                @SuppressWarnings("unchecked")
                List<String> tags = (List<String>) conditions.get("tags");
                List<KnowledgeChunk> tagResults = vectorStoreRepository.findByTags(tags);
                results.addAll(tagResults);
            }
            
            if (conditions.containsKey("id")) {
                String id = (String) conditions.get("id");
                Optional<KnowledgeChunk> idResult = vectorStoreRepository.findById(id);
                idResult.ifPresent(results::add);
            }
            
            // 去重并限制结果数量
            results = results.stream()
                    .distinct()
                    .limit(limit)
                    .collect(Collectors.toList());
            
            // 根据上下文偏好过滤结果
            results = filterByPreferences(results, context);
            
            logger.debug("Condition-based vector search returned {} results", results.size());
            return results;
            
        } catch (Exception e) {
            logger.error("Error during condition-based vector retrieval: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> healthStatus = new HashMap<>();
        
        try {
            boolean isHealthy = vectorStoreRepository.isHealthy();
            healthStatus.put("healthy", isHealthy);
            healthStatus.put("strategy_type", STRATEGY_TYPE);
            healthStatus.put("last_check", new Date());
            
            if (isHealthy) {
                VectorStoreRepository.VectorStoreStats stats = vectorStoreRepository.getStats();
                healthStatus.put("total_count", stats.getTotalCount());
                healthStatus.put("dimensions", stats.getDimensions());
                healthStatus.put("index_type", stats.getIndexType());
            }
            
        } catch (Exception e) {
            logger.error("Error getting vector store health status: {}", e.getMessage(), e);
            healthStatus.put("healthy", false);
            healthStatus.put("error", e.getMessage());
        }
        
        return healthStatus;
    }
    
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            VectorStoreRepository.VectorStoreStats stats = vectorStoreRepository.getStats();
            
            statistics.put("strategy_type", STRATEGY_TYPE);
            statistics.put("total_documents", stats.getTotalCount());
            statistics.put("storage_size_bytes", stats.getStorageSize());
            statistics.put("vector_dimensions", stats.getDimensions());
            statistics.put("index_type", stats.getIndexType());
            statistics.put("default_threshold", DEFAULT_THRESHOLD);
            statistics.put("last_updated", new Date());
            
        } catch (Exception e) {
            logger.error("Error getting vector store statistics: {}", e.getMessage(), e);
            statistics.put("error", e.getMessage());
        }
        
        return statistics;
    }
    
    /**
     * 从上下文中获取相似度阈值
     */
    private double getThresholdFromContext(AgentContext context, double defaultValue) {
        if (context != null && context.getRetrievalParams() != null) {
            Object threshold = context.getRetrievalParams().get("similarity_threshold");
            if (threshold instanceof Number) {
                return ((Number) threshold).doubleValue();
            }
        }
        return defaultValue;
    }
    
    /**
     * 根据上下文偏好过滤结果
     */
    private List<KnowledgeChunk> filterByPreferences(List<KnowledgeChunk> results, AgentContext context) {
        if (context == null || context.getPreferredSources() == null || context.getPreferredSources().isEmpty()) {
            return results;
        }
        
        List<String> preferredSources = context.getPreferredSources();
        
        return results.stream()
                .filter(chunk -> chunk.getSource() == null || preferredSources.contains(chunk.getSource()))
                .collect(Collectors.toList());
    }
    
    /**
     * 根据关键词相关性重新排序结果
     */
    private List<KnowledgeChunk> reorderByKeywordRelevance(List<KnowledgeChunk> results, List<String> keywords) {
        return results.stream()
                .sorted((chunk1, chunk2) -> {
                    int score1 = calculateKeywordScore(chunk1, keywords);
                    int score2 = calculateKeywordScore(chunk2, keywords);
                    return Integer.compare(score2, score1); // 降序排列
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 计算知识块与关键词的匹配分数
     */
    private int calculateKeywordScore(KnowledgeChunk chunk, List<String> keywords) {
        if (chunk.getContent() == null) {
            return 0;
        }
        
        String content = chunk.getContent().toLowerCase();
        int score = 0;
        
        for (String keyword : keywords) {
            String lowerKeyword = keyword.toLowerCase();
            int count = (content.length() - content.replace(lowerKeyword, "").length()) / lowerKeyword.length();
            score += count;
        }
        
        return score;
    }
} 