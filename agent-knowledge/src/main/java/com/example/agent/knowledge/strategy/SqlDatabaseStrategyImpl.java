package com.example.agent.knowledge.strategy;

import com.example.agent.core.dto.AgentContext;
import com.example.agent.core.dto.KnowledgeChunk;
import com.example.agent.core.strategy.KnowledgeSourceStrategy;
import com.example.agent.knowledge.repository.SqlDatabaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SQL数据库检索策略实现
 * 基于SQL查询进行知识检索，支持关键词搜索、条件查询和结构化数据检索
 * 
 * @author agent
 */
@Component
public class SqlDatabaseStrategyImpl implements KnowledgeSourceStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(SqlDatabaseStrategyImpl.class);
    
    private static final String STRATEGY_TYPE = "SQL_DATABASE";
    private static final String DEFAULT_TABLE_NAME = "knowledge_chunks";
    private static final List<String> DEFAULT_SEARCH_FIELDS = Arrays.asList("content", "summary", "tags");
    
    private final SqlDatabaseRepository sqlDatabaseRepository;
    
    @Autowired
    public SqlDatabaseStrategyImpl(SqlDatabaseRepository sqlDatabaseRepository) {
        this.sqlDatabaseRepository = sqlDatabaseRepository;
    }
    
    @Override
    public String getStrategyType() {
        return STRATEGY_TYPE;
    }
    
    @Override
    public boolean isAvailable() {
        try {
            return sqlDatabaseRepository.isHealthy();
        } catch (Exception e) {
            logger.warn("Failed to check SQL database health: {}", e.getMessage());
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
            String tableName = getTableNameFromContext(context, DEFAULT_TABLE_NAME);
            List<String> searchFields = getSearchFieldsFromContext(context, DEFAULT_SEARCH_FIELDS);
            
            logger.debug("Performing SQL keyword search for query: '{}' in table: {} with limit: {}", 
                    query, tableName, limit);
            
            List<KnowledgeChunk> results = sqlDatabaseRepository.searchByKeyword(query, tableName, searchFields, limit);
            
            // 根据上下文偏好过滤结果
            results = filterByPreferences(results, context);
            
            logger.debug("SQL database search returned {} results", results.size());
            return results;
            
        } catch (Exception e) {
            logger.error("Error during SQL database knowledge retrieval: {}", e.getMessage(), e);
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
            String tableName = getTableNameFromContext(context, DEFAULT_TABLE_NAME);
            List<String> searchFields = getSearchFieldsFromContext(context, DEFAULT_SEARCH_FIELDS);
            
            logger.debug("Performing SQL keyword search for keywords: {} in table: {} with limit: {}", 
                    keywords, tableName, limit);
            
            List<KnowledgeChunk> allResults = new ArrayList<>();
            
            // 对每个关键词进行搜索，然后合并结果
            for (String keyword : keywords) {
                List<KnowledgeChunk> keywordResults = sqlDatabaseRepository.searchByKeyword(
                        keyword, tableName, searchFields, limit);
                allResults.addAll(keywordResults);
            }
            
            // 去重并按相关性排序
            allResults = deduplicateAndRankResults(allResults, keywords);
            
            // 限制结果数量
            if (allResults.size() > limit) {
                allResults = allResults.subList(0, limit);
            }
            
            // 根据上下文偏好过滤结果
            allResults = filterByPreferences(allResults, context);
            
            logger.debug("Keyword-based SQL search returned {} results", allResults.size());
            return allResults;
            
        } catch (Exception e) {
            logger.error("Error during keyword-based SQL retrieval: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<KnowledgeChunk> retrieveBySimilarity(List<Double> embedding, AgentContext context, int limit, double threshold) {
        // SQL数据库策略不直接支持向量相似度搜索
        // 可以通过存储的相似度分数进行过滤
        logger.info("SQL database strategy does not support direct embedding similarity search. Using stored similarity scores instead.");
        
        try {
            String tableName = getTableNameFromContext(context, DEFAULT_TABLE_NAME);
            
            Map<String, Object> conditions = new HashMap<>();
            conditions.put("similarity", ">=" + threshold);
            
            List<KnowledgeChunk> results = sqlDatabaseRepository.findByConditions(
                    tableName, conditions, "similarity DESC", limit);
            
            // 根据上下文偏好过滤结果
            results = filterByPreferences(results, context);
            
            logger.debug("Similarity-based SQL search returned {} results", results.size());
            return results;
            
        } catch (Exception e) {
            logger.error("Error during similarity-based SQL retrieval: {}", e.getMessage(), e);
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
            String tableName = getTableNameFromContext(context, DEFAULT_TABLE_NAME);
            String orderBy = getOrderByFromContext(context, "created_at DESC");
            
            logger.debug("Performing SQL condition-based search in table: {} with conditions: {} and limit: {}", 
                    tableName, conditions, limit);
            
            List<KnowledgeChunk> results = sqlDatabaseRepository.findByConditions(
                    tableName, conditions, orderBy, limit);
            
            // 根据上下文偏好过滤结果
            results = filterByPreferences(results, context);
            
            logger.debug("Condition-based SQL search returned {} results", results.size());
            return results;
            
        } catch (Exception e) {
            logger.error("Error during condition-based SQL retrieval: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> healthStatus = new HashMap<>();
        
        try {
            boolean isHealthy = sqlDatabaseRepository.isHealthy();
            healthStatus.put("healthy", isHealthy);
            healthStatus.put("strategy_type", STRATEGY_TYPE);
            healthStatus.put("last_check", new Date());
            
            if (isHealthy) {
                // 获取表信息
                List<String> tableNames = sqlDatabaseRepository.getAllTableNames();
                healthStatus.put("available_tables", tableNames);
                healthStatus.put("table_count", tableNames.size());
                
                // 获取默认表的记录数
                try {
                    Object count = sqlDatabaseRepository.aggregate(DEFAULT_TABLE_NAME, "COUNT", "*", null);
                    healthStatus.put("default_table_records", count);
                } catch (Exception e) {
                    healthStatus.put("default_table_records", "N/A");
                }
            }
            
        } catch (Exception e) {
            logger.error("Error getting SQL database health status: {}", e.getMessage(), e);
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
            statistics.put("default_table", DEFAULT_TABLE_NAME);
            statistics.put("default_search_fields", DEFAULT_SEARCH_FIELDS);
            
            // 获取表统计信息
            List<String> tableNames = sqlDatabaseRepository.getAllTableNames();
            statistics.put("total_tables", tableNames.size());
            statistics.put("available_tables", tableNames);
            
            // 获取默认表的统计信息
            Map<String, Object> tableStats = new HashMap<>();
            try {
                Object totalCount = sqlDatabaseRepository.aggregate(DEFAULT_TABLE_NAME, "COUNT", "*", null);
                tableStats.put("total_records", totalCount);
                
                Object maxId = sqlDatabaseRepository.aggregate(DEFAULT_TABLE_NAME, "MAX", "id", null);
                tableStats.put("max_id", maxId);
                
                Object minCreatedAt = sqlDatabaseRepository.aggregate(DEFAULT_TABLE_NAME, "MIN", "created_at", null);
                tableStats.put("oldest_record", minCreatedAt);
                
            } catch (Exception e) {
                tableStats.put("error", "Failed to get table statistics: " + e.getMessage());
            }
            
            statistics.put("default_table_stats", tableStats);
            statistics.put("last_updated", new Date());
            
        } catch (Exception e) {
            logger.error("Error getting SQL database statistics: {}", e.getMessage(), e);
            statistics.put("error", e.getMessage());
        }
        
        return statistics;
    }
    
    /**
     * 从上下文中获取表名
     */
    private String getTableNameFromContext(AgentContext context, String defaultValue) {
        if (context != null && context.getRetrievalParams() != null) {
            Object tableName = context.getRetrievalParams().get("table_name");
            if (tableName instanceof String) {
                return (String) tableName;
            }
        }
        return defaultValue;
    }
    
    /**
     * 从上下文中获取搜索字段
     */
    @SuppressWarnings("unchecked")
    private List<String> getSearchFieldsFromContext(AgentContext context, List<String> defaultValue) {
        if (context != null && context.getRetrievalParams() != null) {
            Object searchFields = context.getRetrievalParams().get("search_fields");
            if (searchFields instanceof List) {
                return (List<String>) searchFields;
            }
        }
        return defaultValue;
    }
    
    /**
     * 从上下文中获取排序字段
     */
    private String getOrderByFromContext(AgentContext context, String defaultValue) {
        if (context != null && context.getRetrievalParams() != null) {
            Object orderBy = context.getRetrievalParams().get("order_by");
            if (orderBy instanceof String) {
                return (String) orderBy;
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
     * 去重并按相关性排序结果
     */
    private List<KnowledgeChunk> deduplicateAndRankResults(List<KnowledgeChunk> results, List<String> keywords) {
        // 使用LinkedHashMap保持插入顺序，同时去重
        Map<String, KnowledgeChunk> uniqueResults = new LinkedHashMap<>();
        
        for (KnowledgeChunk chunk : results) {
            String key = chunk.getId() != null ? chunk.getId() : 
                        (chunk.getContent() != null ? chunk.getContent().hashCode() + "" : UUID.randomUUID().toString());
            
            if (!uniqueResults.containsKey(key)) {
                uniqueResults.put(key, chunk);
            }
        }
        
        // 按关键词相关性排序
        return uniqueResults.values().stream()
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