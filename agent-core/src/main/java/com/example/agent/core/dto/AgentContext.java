package com.example.agent.core.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Agent 上下文信息 DTO
 * 包含Agent处理过程中的上下文信息，用于策略决策和知识检索
 * 
 * @author agent
 */
public class AgentContext {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 查询ID（用于跟踪单次查询）
     */
    private String queryId;
    
    /**
     * 当前查询文本
     */
    private String currentQuery;
    
    /**
     * 查询意图分类
     */
    private String queryIntent;
    
    /**
     * 对话历史
     */
    private List<String> conversationHistory;
    
    /**
     * 知识源偏好设置
     */
    private List<String> preferredSources;
    
    /**
     * 检索参数配置
     */
    private Map<String, Object> retrievalParams;
    
    /**
     * 用户偏好设置
     */
    private Map<String, Object> userPreferences;
    
    /**
     * 当前步骤名称
     */
    private String currentStep;
    
    /**
     * 处理开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 当前处理时间
     */
    private LocalDateTime currentTime;
    
    /**
     * 处理状态
     */
    private String processingStatus;
    
    /**
     * 错误信息（如果有）
     */
    private String errorMessage;
    
    /**
     * 调试模式标志
     */
    private boolean debugMode;
    
    /**
     * 扩展属性
     */
    private Map<String, Object> extensionProperties;
    
    // 构造函数
    public AgentContext() {
        this.startTime = LocalDateTime.now();
        this.currentTime = LocalDateTime.now();
        this.processingStatus = "INITIALIZED";
        this.debugMode = false;
    }
    
    public AgentContext(String sessionId, String userId, String queryId, String currentQuery) {
        this();
        this.sessionId = sessionId;
        this.userId = userId;
        this.queryId = queryId;
        this.currentQuery = currentQuery;
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getQueryId() {
        return queryId;
    }
    
    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }
    
    public String getCurrentQuery() {
        return currentQuery;
    }
    
    public void setCurrentQuery(String currentQuery) {
        this.currentQuery = currentQuery;
    }
    
    public String getQueryIntent() {
        return queryIntent;
    }
    
    public void setQueryIntent(String queryIntent) {
        this.queryIntent = queryIntent;
    }
    
    public List<String> getConversationHistory() {
        return conversationHistory;
    }
    
    public void setConversationHistory(List<String> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }
    
    public List<String> getPreferredSources() {
        return preferredSources;
    }
    
    public void setPreferredSources(List<String> preferredSources) {
        this.preferredSources = preferredSources;
    }
    
    public Map<String, Object> getRetrievalParams() {
        return retrievalParams;
    }
    
    public void setRetrievalParams(Map<String, Object> retrievalParams) {
        this.retrievalParams = retrievalParams;
    }
    
    public Map<String, Object> getUserPreferences() {
        return userPreferences;
    }
    
    public void setUserPreferences(Map<String, Object> userPreferences) {
        this.userPreferences = userPreferences;
    }
    
    public String getCurrentStep() {
        return currentStep;
    }
    
    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
        this.currentTime = LocalDateTime.now();
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getCurrentTime() {
        return currentTime;
    }
    
    public void setCurrentTime(LocalDateTime currentTime) {
        this.currentTime = currentTime;
    }
    
    public String getProcessingStatus() {
        return processingStatus;
    }
    
    public void setProcessingStatus(String processingStatus) {
        this.processingStatus = processingStatus;
        this.currentTime = LocalDateTime.now();
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public boolean isDebugMode() {
        return debugMode;
    }
    
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
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
        return "AgentContext{" +
                "sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                ", queryId='" + queryId + '\'' +
                ", currentQuery='" + currentQuery + '\'' +
                ", queryIntent='" + queryIntent + '\'' +
                ", currentStep='" + currentStep + '\'' +
                ", processingStatus='" + processingStatus + '\'' +
                ", debugMode=" + debugMode +
                '}';
    }
} 