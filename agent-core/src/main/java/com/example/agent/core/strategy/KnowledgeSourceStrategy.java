package com.example.agent.core.strategy;

import com.example.agent.core.dto.AgentContext;
import com.example.agent.core.dto.KnowledgeChunk;
import java.util.List;
import java.util.Map;

/**
 * 知识源检索策略接口
 * 定义了统一的知识检索API，支持不同类型的知识源
 * 
 * @author agent
 */
public interface KnowledgeSourceStrategy {
    
    /**
     * 获取策略类型标识
     * 
     * @return 策略类型（如："VECTOR_STORE", "SQL_DATABASE", "API_SOURCE"）
     */
    String getStrategyType();
    
    /**
     * 检查策略是否可用
     * 
     * @return true如果策略可用，false否则
     */
    boolean isAvailable();
    
    /**
     * 根据查询文本进行知识检索
     * 
     * @param query 查询文本
     * @param context Agent上下文信息
     * @param limit 返回结果数量限制
     * @return 检索到的知识块列表
     */
    List<KnowledgeChunk> retrieveKnowledge(String query, AgentContext context, int limit);
    
    /**
     * 根据关键词进行知识检索
     * 
     * @param keywords 关键词列表
     * @param context Agent上下文信息
     * @param limit 返回结果数量限制
     * @return 检索到的知识块列表
     */
    List<KnowledgeChunk> retrieveByKeywords(List<String> keywords, AgentContext context, int limit);
    
    /**
     * 根据相似度进行知识检索
     * 
     * @param embedding 查询向量
     * @param context Agent上下文信息
     * @param limit 返回结果数量限制
     * @param threshold 相似度阈值
     * @return 检索到的知识块列表
     */
    List<KnowledgeChunk> retrieveBySimilarity(List<Double> embedding, AgentContext context, int limit, double threshold);
    
    /**
     * 根据条件进行知识检索
     * 
     * @param conditions 检索条件
     * @param context Agent上下文信息
     * @param limit 返回结果数量限制
     * @return 检索到的知识块列表
     */
    List<KnowledgeChunk> retrieveByConditions(Map<String, Object> conditions, AgentContext context, int limit);
    
    /**
     * 获取策略的健康状态
     * 
     * @return 健康状态信息
     */
    Map<String, Object> getHealthStatus();
    
    /**
     * 获取策略的统计信息
     * 
     * @return 统计信息
     */
    Map<String, Object> getStatistics();
} 