package com.example.agent.knowledge.strategy;

import com.example.agent.core.dto.AgentContext;
import com.example.agent.core.dto.KnowledgeChunk;
import com.example.agent.core.strategy.KnowledgeSourceStrategy;
import com.example.agent.knowledge.repository.ApiSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 外部API检索策略实现
 * 基于外部API进行知识检索，支持多种API源类型和异步调用
 * 
 * @author agent
 */
@Component
public class ApiSourceStrategyImpl implements KnowledgeSourceStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiSourceStrategyImpl.class);
    
    private static final String STRATEGY_TYPE = "API_SOURCE";
    private static final int DEFAULT_TIMEOUT_MS = 30000;
    private static final int DEFAULT_ASYNC_TIMEOUT_SECONDS = 60;
    
    private final ApiSourceRepository apiSourceRepository;
    
    @Autowired
    public ApiSourceStrategyImpl(ApiSourceRepository apiSourceRepository) {
        this.apiSourceRepository = apiSourceRepository;
    }
    
    @Override
    public String getStrategyType() {
        return STRATEGY_TYPE;
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // 检查支持的API源类型是否可用
            List<String> supportedTypes = apiSourceRepository.getSupportedSourceTypes();
            return supportedTypes != null && !supportedTypes.isEmpty();
        } catch (Exception e) {
            logger.warn("Failed to check API source availability: {}", e.getMessage());
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
            List<ApiSourceRepository.ApiSourceConfig> sourceConfigs = getSourceConfigsFromContext(context);
            
            if (sourceConfigs.isEmpty()) {
                logger.warn("No API source configurations found in context");
                return new ArrayList<>();
            }
            
            logger.debug("Performing API search for query: '{}' across {} sources with limit: {}", 
                    query, sourceConfigs.size(), limit);
            
            List<KnowledgeChunk> allResults = new ArrayList<>();
            
            // 对每个API源进行搜索
            for (ApiSourceRepository.ApiSourceConfig config : sourceConfigs) {
                try {
                    List<KnowledgeChunk> sourceResults = apiSourceRepository.searchExternalSource(config, query, limit);
                    allResults.addAll(sourceResults);
                } catch (Exception e) {
                    logger.error("Error searching API source {}: {}", config.getSourceId(), e.getMessage(), e);
                }
            }
            
            // 去重、排序并限制结果数量
            allResults = processAndLimitResults(allResults, query, limit);
            
            // 根据上下文偏好过滤结果
            allResults = filterByPreferences(allResults, context);
            
            logger.debug("API source search returned {} results", allResults.size());
            return allResults;
            
        } catch (Exception e) {
            logger.error("Error during API source knowledge retrieval: {}", e.getMessage(), e);
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
            // 将关键词组合成查询字符串
            String combinedQuery = String.join(" ", keywords);
            
            List<ApiSourceRepository.ApiSourceConfig> sourceConfigs = getSourceConfigsFromContext(context);
            
            if (sourceConfigs.isEmpty()) {
                logger.warn("No API source configurations found in context");
                return new ArrayList<>();
            }
            
            logger.debug("Performing API keyword search for: {} across {} sources with limit: {}", 
                    keywords, sourceConfigs.size(), limit);
            
            List<KnowledgeChunk> allResults = new ArrayList<>();
            
            // 支持异步搜索以提高性能
            boolean useAsync = shouldUseAsyncFromContext(context, sourceConfigs.size() > 1);
            
            if (useAsync) {
                allResults = performAsyncKeywordSearch(sourceConfigs, combinedQuery, limit);
            } else {
                allResults = performSyncKeywordSearch(sourceConfigs, combinedQuery, limit);
            }
            
            // 根据关键词相关性重新排序
            allResults = reorderByKeywordRelevance(allResults, keywords);
            
            // 去重并限制结果数量
            allResults = processAndLimitResults(allResults, combinedQuery, limit);
            
            // 根据上下文偏好过滤结果
            allResults = filterByPreferences(allResults, context);
            
            logger.debug("Keyword-based API search returned {} results", allResults.size());
            return allResults;
            
        } catch (Exception e) {
            logger.error("Error during keyword-based API retrieval: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<KnowledgeChunk> retrieveBySimilarity(List<Double> embedding, AgentContext context, int limit, double threshold) {
        // API源策略通常不直接支持向量相似度搜索
        // 但可以通过特定的API端点实现，例如调用向量搜索API
        logger.info("API source strategy attempting similarity search through specialized API endpoints");
        
        try {
            List<ApiSourceRepository.ApiSourceConfig> sourceConfigs = getSourceConfigsFromContext(context);
            List<KnowledgeChunk> results = new ArrayList<>();
            
            for (ApiSourceRepository.ApiSourceConfig config : sourceConfigs) {
                // 检查API源是否支持向量搜索
                if (supportsVectorSearch(config)) {
                    try {
                        List<KnowledgeChunk> vectorResults = performVectorSearchAPI(config, embedding, limit, threshold);
                        results.addAll(vectorResults);
                    } catch (Exception e) {
                        logger.error("Error during vector search for API source {}: {}", config.getSourceId(), e.getMessage());
                    }
                }
            }
            
            // 根据相似度排序
            results = results.stream()
                    .filter(chunk -> chunk.getSimilarity() != null && chunk.getSimilarity() >= threshold)
                    .sorted((chunk1, chunk2) -> Double.compare(chunk2.getSimilarity(), chunk1.getSimilarity()))
                    .limit(limit)
                    .collect(Collectors.toList());
            
            // 根据上下文偏好过滤结果
            results = filterByPreferences(results, context);
            
            logger.debug("Similarity-based API search returned {} results", results.size());
            return results;
            
        } catch (Exception e) {
            logger.error("Error during similarity-based API retrieval: {}", e.getMessage(), e);
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
            List<ApiSourceRepository.ApiSourceConfig> sourceConfigs = getSourceConfigsFromContext(context);
            
            if (sourceConfigs.isEmpty()) {
                logger.warn("No API source configurations found in context");
                return new ArrayList<>();
            }
            
            logger.debug("Performing API condition-based search with conditions: {} across {} sources with limit: {}", 
                    conditions, sourceConfigs.size(), limit);
            
            List<KnowledgeChunk> allResults = new ArrayList<>();
            
            for (ApiSourceRepository.ApiSourceConfig config : sourceConfigs) {
                try {
                    List<KnowledgeChunk> sourceResults = performConditionBasedSearch(config, conditions, limit);
                    allResults.addAll(sourceResults);
                } catch (Exception e) {
                    logger.error("Error in condition-based search for API source {}: {}", config.getSourceId(), e.getMessage());
                }
            }
            
            // 去重并限制结果数量
            allResults = allResults.stream()
                    .distinct()
                    .limit(limit)
                    .collect(Collectors.toList());
            
            // 根据上下文偏好过滤结果
            allResults = filterByPreferences(allResults, context);
            
            logger.debug("Condition-based API search returned {} results", allResults.size());
            return allResults;
            
        } catch (Exception e) {
            logger.error("Error during condition-based API retrieval: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> healthStatus = new HashMap<>();
        
        try {
            boolean isAvailable = isAvailable();
            healthStatus.put("healthy", isAvailable);
            healthStatus.put("strategy_type", STRATEGY_TYPE);
            healthStatus.put("last_check", new Date());
            
            if (isAvailable) {
                List<String> supportedTypes = apiSourceRepository.getSupportedSourceTypes();
                healthStatus.put("supported_types", supportedTypes);
                healthStatus.put("supported_types_count", supportedTypes.size());
            }
            
        } catch (Exception e) {
            logger.error("Error getting API source health status: {}", e.getMessage(), e);
            healthStatus.put("healthy", false);
            healthStatus.put("error", e.getMessage());
        }
        
        return healthStatus;
    }
    
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            statistics.put("strategy_type", STRATEGY_TYPE);
            statistics.put("default_timeout_ms", DEFAULT_TIMEOUT_MS);
            statistics.put("async_timeout_seconds", DEFAULT_ASYNC_TIMEOUT_SECONDS);
            
            List<String> supportedTypes = apiSourceRepository.getSupportedSourceTypes();
            statistics.put("supported_source_types", supportedTypes);
            statistics.put("total_supported_types", supportedTypes.size());
            
            // 获取各个API源的使用统计
            Map<String, Object> usageStats = new HashMap<>();
            // 这里可以根据实际需要添加特定API源的统计信息
            
            statistics.put("usage_statistics", usageStats);
            statistics.put("last_updated", new Date());
            
        } catch (Exception e) {
            logger.error("Error getting API source statistics: {}", e.getMessage(), e);
            statistics.put("error", e.getMessage());
        }
        
        return statistics;
    }
    
    /**
     * 从上下文中获取API源配置
     */
    @SuppressWarnings("unchecked")
    private List<ApiSourceRepository.ApiSourceConfig> getSourceConfigsFromContext(AgentContext context) {
        List<ApiSourceRepository.ApiSourceConfig> configs = new ArrayList<>();
        
        if (context != null && context.getRetrievalParams() != null) {
            Object apiConfigs = context.getRetrievalParams().get("api_source_configs");
            if (apiConfigs instanceof List) {
                List<Map<String, Object>> configMaps = (List<Map<String, Object>>) apiConfigs;
                for (Map<String, Object> configMap : configMaps) {
                    configs.add(mapToApiSourceConfig(configMap));
                }
            }
        }
        
        // 如果上下文中没有配置，使用默认配置
        if (configs.isEmpty()) {
            configs.add(createDefaultApiSourceConfig());
        }
        
        return configs;
    }
    
    /**
     * 将Map转换为ApiSourceConfig
     */
    private ApiSourceRepository.ApiSourceConfig mapToApiSourceConfig(Map<String, Object> configMap) {
        ApiSourceRepository.ApiSourceConfig config = new ApiSourceRepository.ApiSourceConfig();
        config.setSourceId((String) configMap.get("sourceId"));
        config.setSourceType((String) configMap.get("sourceType"));
        config.setBaseUrl((String) configMap.get("baseUrl"));
        
        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) configMap.get("defaultHeaders");
        config.setDefaultHeaders(headers);
        
        if (configMap.containsKey("timeoutMs")) {
            config.setTimeoutMs((Integer) configMap.get("timeoutMs"));
        }
        
        return config;
    }
    
    /**
     * 创建默认API源配置
     */
    private ApiSourceRepository.ApiSourceConfig createDefaultApiSourceConfig() {
        ApiSourceRepository.ApiSourceConfig config = new ApiSourceRepository.ApiSourceConfig();
        config.setSourceId("default");
        config.setSourceType("generic");
        config.setBaseUrl("http://localhost:8080/api");
        config.setTimeoutMs(DEFAULT_TIMEOUT_MS);
        return config;
    }
    
    /**
     * 判断是否应该使用异步搜索
     */
    private boolean shouldUseAsyncFromContext(AgentContext context, boolean defaultValue) {
        if (context != null && context.getRetrievalParams() != null) {
            Object useAsync = context.getRetrievalParams().get("use_async");
            if (useAsync instanceof Boolean) {
                return (Boolean) useAsync;
            }
        }
        return defaultValue;
    }
    
    /**
     * 执行同步关键词搜索
     */
    private List<KnowledgeChunk> performSyncKeywordSearch(List<ApiSourceRepository.ApiSourceConfig> sourceConfigs, 
                                                         String query, int limit) {
        List<KnowledgeChunk> allResults = new ArrayList<>();
        
        for (ApiSourceRepository.ApiSourceConfig config : sourceConfigs) {
            try {
                List<KnowledgeChunk> sourceResults = apiSourceRepository.searchExternalSource(config, query, limit);
                allResults.addAll(sourceResults);
            } catch (Exception e) {
                logger.error("Error in sync search for API source {}: {}", config.getSourceId(), e.getMessage());
            }
        }
        
        return allResults;
    }
    
    /**
     * 执行异步关键词搜索
     */
    private List<KnowledgeChunk> performAsyncKeywordSearch(List<ApiSourceRepository.ApiSourceConfig> sourceConfigs, 
                                                          String query, int limit) {
        List<CompletableFuture<List<KnowledgeChunk>>> futures = new ArrayList<>();
        
        for (ApiSourceRepository.ApiSourceConfig config : sourceConfigs) {
            CompletableFuture<List<KnowledgeChunk>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return apiSourceRepository.searchExternalSource(config, query, limit);
                } catch (Exception e) {
                    logger.error("Error in async search for API source {}: {}", config.getSourceId(), e.getMessage());
                    return new ArrayList<>();
                }
            });
            futures.add(future);
        }
        
        // 等待所有异步任务完成
        List<KnowledgeChunk> allResults = new ArrayList<>();
        try {
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allFutures.get(DEFAULT_ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            for (CompletableFuture<List<KnowledgeChunk>> future : futures) {
                try {
                    allResults.addAll(future.get());
                } catch (Exception e) {
                    logger.error("Error getting async search result: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error waiting for async search completion: {}", e.getMessage());
        }
        
        return allResults;
    }
    
    /**
     * 检查API源是否支持向量搜索
     */
    private boolean supportsVectorSearch(ApiSourceRepository.ApiSourceConfig config) {
        try {
            ApiSourceRepository.ApiSourceMetadata metadata = apiSourceRepository.getSourceMetadata(config);
            return metadata.getSupportedOperations() != null && 
                   metadata.getSupportedOperations().contains("vector_search");
        } catch (Exception e) {
            logger.debug("Cannot determine vector search support for source {}: {}", config.getSourceId(), e.getMessage());
            return false;
        }
    }
    
    /**
     * 执行向量搜索API调用
     */
    private List<KnowledgeChunk> performVectorSearchAPI(ApiSourceRepository.ApiSourceConfig config, 
                                                       List<Double> embedding, int limit, double threshold) {
        // 这里应该实现具体的向量搜索API调用逻辑
        // 为了示例，返回空列表
        logger.info("Vector search API call for source {} not yet implemented", config.getSourceId());
        return new ArrayList<>();
    }
    
    /**
     * 执行基于条件的搜索
     */
    private List<KnowledgeChunk> performConditionBasedSearch(ApiSourceRepository.ApiSourceConfig config, 
                                                           Map<String, Object> conditions, int limit) {
        // 构建API查询参数
        Map<String, Object> queryParams = new HashMap<>(conditions);
        queryParams.put("limit", limit);
        
        try {
            return apiSourceRepository.get(config.getBaseUrl() + "/search", 
                                         config.getDefaultHeaders(), queryParams);
        } catch (Exception e) {
            logger.error("Error in condition-based search for API source {}: {}", config.getSourceId(), e.getMessage());
            return new ArrayList<>();
        }
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
     * 处理并限制结果数量
     */
    private List<KnowledgeChunk> processAndLimitResults(List<KnowledgeChunk> results, String query, int limit) {
        return results.stream()
                .distinct()
                .sorted((chunk1, chunk2) -> {
                    // 按相似度或创建时间排序
                    if (chunk1.getSimilarity() != null && chunk2.getSimilarity() != null) {
                        return Double.compare(chunk2.getSimilarity(), chunk1.getSimilarity());
                    }
                    if (chunk1.getCreatedAt() != null && chunk2.getCreatedAt() != null) {
                        return chunk2.getCreatedAt().compareTo(chunk1.getCreatedAt());
                    }
                    return 0;
                })
                .limit(limit)
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
        
        // 额外考虑摘要和标签中的匹配
        if (chunk.getSummary() != null) {
            String summary = chunk.getSummary().toLowerCase();
            for (String keyword : keywords) {
                String lowerKeyword = keyword.toLowerCase();
                if (summary.contains(lowerKeyword)) {
                    score += 2; // 摘要匹配权重更高
                }
            }
        }
        
        if (chunk.getTags() != null) {
            for (String tag : chunk.getTags()) {
                String lowerTag = tag.toLowerCase();
                for (String keyword : keywords) {
                    String lowerKeyword = keyword.toLowerCase();
                    if (lowerTag.contains(lowerKeyword)) {
                        score += 3; // 标签匹配权重最高
                    }
                }
            }
        }
        
        return score;
    }
} 