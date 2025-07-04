package com.example.agent.knowledge.factory;

import com.example.agent.core.dto.AgentContext;
import com.example.agent.core.strategy.KnowledgeSourceStrategy;
import com.example.agent.knowledge.strategy.ApiSourceStrategyImpl;
import com.example.agent.knowledge.strategy.SqlDatabaseStrategyImpl;
import com.example.agent.knowledge.strategy.VectorStoreStrategyImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识源策略工厂
 * 负责根据上下文和配置选择和创建合适的知识检索策略
 * 
 * @author agent
 */
@Component
public class KnowledgeSourceFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeSourceFactory.class);
    
    private final VectorStoreStrategyImpl vectorStoreStrategy;
    private final SqlDatabaseStrategyImpl sqlDatabaseStrategy;
    private final ApiSourceStrategyImpl apiSourceStrategy;
    
    // 策略映射
    private final Map<String, KnowledgeSourceStrategy> strategyMap;
    
    @Autowired
    public KnowledgeSourceFactory(VectorStoreStrategyImpl vectorStoreStrategy,
                                 SqlDatabaseStrategyImpl sqlDatabaseStrategy,
                                 ApiSourceStrategyImpl apiSourceStrategy) {
        this.vectorStoreStrategy = vectorStoreStrategy;
        this.sqlDatabaseStrategy = sqlDatabaseStrategy;
        this.apiSourceStrategy = apiSourceStrategy;
        
        // 初始化策略映射
        this.strategyMap = new HashMap<>();
        this.strategyMap.put("VECTOR_STORE", vectorStoreStrategy);
        this.strategyMap.put("SQL_DATABASE", sqlDatabaseStrategy);
        this.strategyMap.put("API_SOURCE", apiSourceStrategy);
    }
    
    /**
     * 根据策略类型获取单个策略
     * 
     * @param strategyType 策略类型
     * @return 对应的策略实例，如果不存在则返回null
     */
    public KnowledgeSourceStrategy getStrategy(String strategyType) {
        if (strategyType == null || strategyType.trim().isEmpty()) {
            logger.warn("Strategy type is null or empty");
            return null;
        }
        
        KnowledgeSourceStrategy strategy = strategyMap.get(strategyType.toUpperCase());
        
        if (strategy == null) {
            logger.warn("Unknown strategy type: {}", strategyType);
            return null;
        }
        
        // 检查策略是否可用
        if (!strategy.isAvailable()) {
            logger.warn("Strategy {} is not available", strategyType);
            return null;
        }
        
        return strategy;
    }
    
    /**
     * 根据上下文自动选择最合适的策略
     * 
     * @param context Agent上下文
     * @return 最合适的策略实例
     */
    public KnowledgeSourceStrategy selectOptimalStrategy(AgentContext context) {
        if (context == null) {
            logger.info("Context is null, using default strategy");
            return getDefaultStrategy();
        }
        
        // 从上下文中获取偏好设置
        List<String> preferredSources = context.getPreferredSources();
        if (preferredSources != null && !preferredSources.isEmpty()) {
            // 根据偏好选择策略
            for (String source : preferredSources) {
                KnowledgeSourceStrategy strategy = getStrategy(source);
                if (strategy != null) {
                    logger.debug("Selected strategy {} based on user preferences", source);
                    return strategy;
                }
            }
        }
        
        // 根据查询类型自动选择策略
        String queryIntent = context.getQueryIntent();
        if (queryIntent != null) {
            KnowledgeSourceStrategy strategy = selectStrategyByIntent(queryIntent);
            if (strategy != null) {
                logger.debug("Selected strategy based on query intent: {}", queryIntent);
                return strategy;
            }
        }
        
        // 根据上下文参数选择策略
        Map<String, Object> retrievalParams = context.getRetrievalParams();
        if (retrievalParams != null) {
            String strategyHint = (String) retrievalParams.get("strategy_hint");
            if (strategyHint != null) {
                KnowledgeSourceStrategy strategy = getStrategy(strategyHint);
                if (strategy != null) {
                    logger.debug("Selected strategy {} based on context hint", strategyHint);
                    return strategy;
                }
            }
        }
        
        // 使用默认策略
        logger.debug("Using default strategy");
        return getDefaultStrategy();
    }
    
    /**
     * 获取多个可用策略（用于并行检索）
     * 
     * @param context Agent上下文
     * @param maxStrategies 最大策略数量
     * @return 可用策略列表
     */
    public List<KnowledgeSourceStrategy> selectMultipleStrategies(AgentContext context, int maxStrategies) {
        List<KnowledgeSourceStrategy> strategies = new ArrayList<>();
        
        // 获取所有可用策略
        List<KnowledgeSourceStrategy> availableStrategies = getAllAvailableStrategies();
        
        if (context != null && context.getPreferredSources() != null) {
            // 根据偏好排序
            List<String> preferredSources = context.getPreferredSources();
            
            // 首先添加偏好的策略
            for (String source : preferredSources) {
                KnowledgeSourceStrategy strategy = getStrategy(source);
                if (strategy != null && !strategies.contains(strategy)) {
                    strategies.add(strategy);
                    if (strategies.size() >= maxStrategies) {
                        break;
                    }
                }
            }
            
            // 如果还需要更多策略，添加其他可用策略
            for (KnowledgeSourceStrategy strategy : availableStrategies) {
                if (!strategies.contains(strategy)) {
                    strategies.add(strategy);
                    if (strategies.size() >= maxStrategies) {
                        break;
                    }
                }
            }
        } else {
            // 如果没有偏好设置，按默认顺序返回策略
            strategies.addAll(availableStrategies.subList(0, Math.min(maxStrategies, availableStrategies.size())));
        }
        
        logger.debug("Selected {} strategies for parallel retrieval", strategies.size());
        return strategies;
    }
    
    /**
     * 获取所有可用的策略
     * 
     * @return 所有可用策略列表
     */
    public List<KnowledgeSourceStrategy> getAllAvailableStrategies() {
        return strategyMap.values().stream()
                .filter(KnowledgeSourceStrategy::isAvailable)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有支持的策略类型
     * 
     * @return 支持的策略类型列表
     */
    public List<String> getSupportedStrategyTypes() {
        return new ArrayList<>(strategyMap.keySet());
    }
    
    /**
     * 检查指定策略是否可用
     * 
     * @param strategyType 策略类型
     * @return 是否可用
     */
    public boolean isStrategyAvailable(String strategyType) {
        KnowledgeSourceStrategy strategy = strategyMap.get(strategyType);
        return strategy != null && strategy.isAvailable();
    }
    
    /**
     * 获取策略健康状态
     * 
     * @return 所有策略的健康状态
     */
    public Map<String, Map<String, Object>> getStrategiesHealthStatus() {
        Map<String, Map<String, Object>> healthStatus = new HashMap<>();
        
        for (Map.Entry<String, KnowledgeSourceStrategy> entry : strategyMap.entrySet()) {
            String strategyType = entry.getKey();
            KnowledgeSourceStrategy strategy = entry.getValue();
            
            try {
                healthStatus.put(strategyType, strategy.getHealthStatus());
            } catch (Exception e) {
                Map<String, Object> errorStatus = new HashMap<>();
                errorStatus.put("healthy", false);
                errorStatus.put("error", e.getMessage());
                healthStatus.put(strategyType, errorStatus);
            }
        }
        
        return healthStatus;
    }
    
    /**
     * 获取策略统计信息
     * 
     * @return 所有策略的统计信息
     */
    public Map<String, Map<String, Object>> getStrategiesStatistics() {
        Map<String, Map<String, Object>> statistics = new HashMap<>();
        
        for (Map.Entry<String, KnowledgeSourceStrategy> entry : strategyMap.entrySet()) {
            String strategyType = entry.getKey();
            KnowledgeSourceStrategy strategy = entry.getValue();
            
            try {
                statistics.put(strategyType, strategy.getStatistics());
            } catch (Exception e) {
                Map<String, Object> errorStats = new HashMap<>();
                errorStats.put("error", e.getMessage());
                statistics.put(strategyType, errorStats);
            }
        }
        
        return statistics;
    }
    
    /**
     * 获取默认策略
     * 
     * @return 默认策略实例
     */
    private KnowledgeSourceStrategy getDefaultStrategy() {
        // 按优先级顺序尝试获取可用策略
        String[] defaultOrder = {"VECTOR_STORE", "SQL_DATABASE", "API_SOURCE"};
        
        for (String strategyType : defaultOrder) {
            KnowledgeSourceStrategy strategy = getStrategy(strategyType);
            if (strategy != null) {
                return strategy;
            }
        }
        
        logger.error("No available strategies found!");
        return null;
    }
    
    /**
     * 根据查询意图选择策略
     * 
     * @param queryIntent 查询意图
     * @return 合适的策略实例
     */
    private KnowledgeSourceStrategy selectStrategyByIntent(String queryIntent) {
        if (queryIntent == null) {
            return null;
        }
        
        String intent = queryIntent.toLowerCase();
        
        // 语义搜索和相似度查询优先使用向量存储
        if (intent.contains("semantic") || intent.contains("similar") || intent.contains("embedding")) {
            return getStrategy("VECTOR_STORE");
        }
        
        // 结构化查询和精确搜索优先使用SQL数据库
        if (intent.contains("sql") || intent.contains("structured") || intent.contains("exact") || intent.contains("filter")) {
            return getStrategy("SQL_DATABASE");
        }
        
        // 外部数据源和实时信息优先使用API源
        if (intent.contains("external") || intent.contains("api") || intent.contains("realtime") || intent.contains("latest")) {
            return getStrategy("API_SOURCE");
        }
        
        return null;
    }
    
    /**
     * 创建策略选择报告
     * 
     * @param context Agent上下文
     * @return 策略选择详情
     */
    public Map<String, Object> createSelectionReport(AgentContext context) {
        Map<String, Object> report = new HashMap<>();
        
        report.put("timestamp", new Date());
        report.put("context_session_id", context != null ? context.getSessionId() : null);
        report.put("context_query_id", context != null ? context.getQueryId() : null);
        
        // 可用策略信息
        List<String> availableStrategies = getAllAvailableStrategies().stream()
                .map(KnowledgeSourceStrategy::getStrategyType)
                .collect(Collectors.toList());
        report.put("available_strategies", availableStrategies);
        
        // 选中策略信息
        KnowledgeSourceStrategy selectedStrategy = selectOptimalStrategy(context);
        report.put("selected_strategy", selectedStrategy != null ? selectedStrategy.getStrategyType() : "NONE");
        
        // 选择原因
        report.put("selection_reason", determineSelectionReason(context, selectedStrategy));
        
        return report;
    }
    
    /**
     * 确定策略选择原因
     */
    private String determineSelectionReason(AgentContext context, KnowledgeSourceStrategy selectedStrategy) {
        if (selectedStrategy == null) {
            return "No available strategies";
        }
        
        if (context == null) {
            return "Default strategy (no context)";
        }
        
        if (context.getPreferredSources() != null && context.getPreferredSources().contains(selectedStrategy.getStrategyType())) {
            return "User preference";
        }
        
        if (context.getQueryIntent() != null) {
            return "Query intent matching";
        }
        
        if (context.getRetrievalParams() != null && context.getRetrievalParams().containsKey("strategy_hint")) {
            return "Context hint";
        }
        
        return "Default fallback";
    }
} 