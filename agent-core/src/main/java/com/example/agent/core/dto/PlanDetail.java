package com.example.agent.core.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 计划详情 DTO
 * 包含查询规划的详细信息
 * 
 * @author agent
 */
public class PlanDetail {
    
    /**
     * 计划ID
     */
    private String planId;
    
    /**
     * 查询分析结果
     */
    private String queryAnalysis;
    
    /**
     * 查询意图
     */
    private String queryIntent;
    
    /**
     * 查询类型（问答、搜索、推理等）
     */
    private String queryType;
    
    /**
     * 知识源策略
     */
    private List<String> knowledgeSourceStrategies;
    
    /**
     * 检索参数
     */
    private Map<String, Object> retrievalParameters;
    
    /**
     * 预期步骤序列
     */
    private List<String> expectedSteps;
    
    /**
     * 优先级
     */
    private int priority;
    
    /**
     * 预估耗时（毫秒）
     */
    private long estimatedDuration;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 扩展属性
     */
    private Map<String, Object> extensionProperties;
    
    // 构造函数
    public PlanDetail() {
        this.createdAt = LocalDateTime.now();
        this.priority = 1;
        this.estimatedDuration = 5000; // 默认5秒
    }
    
    public PlanDetail(String planId, String queryAnalysis, String queryIntent) {
        this();
        this.planId = planId;
        this.queryAnalysis = queryAnalysis;
        this.queryIntent = queryIntent;
    }
    
    // Getters and Setters
    public String getPlanId() {
        return planId;
    }
    
    public void setPlanId(String planId) {
        this.planId = planId;
    }
    
    public String getQueryAnalysis() {
        return queryAnalysis;
    }
    
    public void setQueryAnalysis(String queryAnalysis) {
        this.queryAnalysis = queryAnalysis;
    }
    
    public String getQueryIntent() {
        return queryIntent;
    }
    
    public void setQueryIntent(String queryIntent) {
        this.queryIntent = queryIntent;
    }
    
    public String getQueryType() {
        return queryType;
    }
    
    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }
    
    public List<String> getKnowledgeSourceStrategies() {
        return knowledgeSourceStrategies;
    }
    
    public void setKnowledgeSourceStrategies(List<String> knowledgeSourceStrategies) {
        this.knowledgeSourceStrategies = knowledgeSourceStrategies;
    }
    
    public Map<String, Object> getRetrievalParameters() {
        return retrievalParameters;
    }
    
    public void setRetrievalParameters(Map<String, Object> retrievalParameters) {
        this.retrievalParameters = retrievalParameters;
    }
    
    public List<String> getExpectedSteps() {
        return expectedSteps;
    }
    
    public void setExpectedSteps(List<String> expectedSteps) {
        this.expectedSteps = expectedSteps;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public long getEstimatedDuration() {
        return estimatedDuration;
    }
    
    public void setEstimatedDuration(long estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Map<String, Object> getExtensionProperties() {
        return extensionProperties;
    }
    
    public void setExtensionProperties(Map<String, Object> extensionProperties) {
        this.extensionProperties = extensionProperties;
    }
    
    // 便利方法
    public void addExtensionProperty(String key, Object value) {
        if (this.extensionProperties == null) {
            this.extensionProperties = new java.util.HashMap<>();
        }
        this.extensionProperties.put(key, value);
    }
    
    public Object getExtensionProperty(String key) {
        return this.extensionProperties != null ? this.extensionProperties.get(key) : null;
    }
    
    @Override
    public String toString() {
        return "PlanDetail{" +
                "planId='" + planId + '\'' +
                ", queryAnalysis='" + queryAnalysis + '\'' +
                ", queryIntent='" + queryIntent + '\'' +
                ", queryType='" + queryType + '\'' +
                ", knowledgeSourceStrategies=" + knowledgeSourceStrategies +
                ", priority=" + priority +
                ", estimatedDuration=" + estimatedDuration +
                ", createdAt=" + createdAt +
                '}';
    }
} 