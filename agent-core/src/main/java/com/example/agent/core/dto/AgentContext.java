package com.example.agent.core.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    
    /**
     * 私有构造函数，用于Builder
     */
    private AgentContext(Builder builder) {
        this.sessionId = builder.sessionId;
        this.userId = builder.userId;
        this.queryId = builder.queryId;
        this.currentQuery = builder.currentQuery;
        this.queryIntent = builder.queryIntent;
        this.conversationHistory = builder.conversationHistory != null ? 
            new ArrayList<>(builder.conversationHistory) : null;
        this.preferredSources = builder.preferredSources != null ? 
            new ArrayList<>(builder.preferredSources) : null;
        this.retrievalParams = builder.retrievalParams != null ? 
            new HashMap<>(builder.retrievalParams) : null;
        this.userPreferences = builder.userPreferences != null ? 
            new HashMap<>(builder.userPreferences) : null;
        this.currentStep = builder.currentStep;
        this.startTime = builder.startTime != null ? builder.startTime : LocalDateTime.now();
        this.currentTime = builder.currentTime != null ? builder.currentTime : LocalDateTime.now();
        this.processingStatus = builder.processingStatus != null ? builder.processingStatus : "INITIALIZED";
        this.errorMessage = builder.errorMessage;
        this.debugMode = builder.debugMode;
        this.extensionProperties = builder.extensionProperties != null ? 
            new HashMap<>(builder.extensionProperties) : null;
    }
    
    /**
     * 创建Builder实例
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 从现有AgentContext创建Builder
     */
    public static Builder builder(AgentContext context) {
        return new Builder(context);
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (conversationHistory != null) {
            conversationHistory.clear();
        }
        if (preferredSources != null) {
            preferredSources.clear();
        }
        if (retrievalParams != null) {
            retrievalParams.clear();
        }
        if (userPreferences != null) {
            userPreferences.clear();
        }
        if (extensionProperties != null) {
            extensionProperties.clear();
        }
    }
    
    /**
     * 复制当前上下文创建新实例
     */
    public AgentContext copy() {
        return builder(this).build();
    }
    
    /**
     * 验证上下文是否有效
     */
    public boolean isValid() {
        return sessionId != null && !sessionId.trim().isEmpty() &&
               userId != null && !userId.trim().isEmpty() &&
               queryId != null && !queryId.trim().isEmpty();
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
            this.extensionProperties = new HashMap<>();
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentContext that = (AgentContext) o;
        return debugMode == that.debugMode &&
                Objects.equals(sessionId, that.sessionId) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(queryId, that.queryId) &&
                Objects.equals(currentQuery, that.currentQuery);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(sessionId, userId, queryId, currentQuery, debugMode);
    }
    
    /**
     * AgentContext Builder类
     * 提供流式API构建AgentContext实例，支持验证和状态传递
     */
    public static class Builder {
        private String sessionId;
        private String userId;
        private String queryId;
        private String currentQuery;
        private String queryIntent;
        private List<String> conversationHistory;
        private List<String> preferredSources;
        private Map<String, Object> retrievalParams;
        private Map<String, Object> userPreferences;
        private String currentStep;
        private LocalDateTime startTime;
        private LocalDateTime currentTime;
        private String processingStatus;
        private String errorMessage;
        private boolean debugMode;
        private Map<String, Object> extensionProperties;
        
        /**
         * 默认构造函数
         */
        public Builder() {
            this.debugMode = false;
        }
        
        /**
         * 从现有AgentContext构造Builder
         */
        public Builder(AgentContext context) {
            if (context != null) {
                this.sessionId = context.sessionId;
                this.userId = context.userId;
                this.queryId = context.queryId;
                this.currentQuery = context.currentQuery;
                this.queryIntent = context.queryIntent;
                this.conversationHistory = context.conversationHistory != null ? 
                    new ArrayList<>(context.conversationHistory) : null;
                this.preferredSources = context.preferredSources != null ? 
                    new ArrayList<>(context.preferredSources) : null;
                this.retrievalParams = context.retrievalParams != null ? 
                    new HashMap<>(context.retrievalParams) : null;
                this.userPreferences = context.userPreferences != null ? 
                    new HashMap<>(context.userPreferences) : null;
                this.currentStep = context.currentStep;
                this.startTime = context.startTime;
                this.currentTime = context.currentTime;
                this.processingStatus = context.processingStatus;
                this.errorMessage = context.errorMessage;
                this.debugMode = context.debugMode;
                this.extensionProperties = context.extensionProperties != null ? 
                    new HashMap<>(context.extensionProperties) : null;
            }
        }
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder queryId(String queryId) {
            this.queryId = queryId;
            return this;
        }
        
        public Builder currentQuery(String currentQuery) {
            this.currentQuery = currentQuery;
            return this;
        }
        
        public Builder queryIntent(String queryIntent) {
            this.queryIntent = queryIntent;
            return this;
        }
        
        public Builder conversationHistory(List<String> conversationHistory) {
            this.conversationHistory = conversationHistory != null ? 
                new ArrayList<>(conversationHistory) : null;
            return this;
        }
        
        public Builder addConversationHistory(String message) {
            if (this.conversationHistory == null) {
                this.conversationHistory = new ArrayList<>();
            }
            this.conversationHistory.add(message);
            return this;
        }
        
        public Builder preferredSources(List<String> preferredSources) {
            this.preferredSources = preferredSources != null ? 
                new ArrayList<>(preferredSources) : null;
            return this;
        }
        
        public Builder addPreferredSource(String source) {
            if (this.preferredSources == null) {
                this.preferredSources = new ArrayList<>();
            }
            this.preferredSources.add(source);
            return this;
        }
        
        public Builder retrievalParams(Map<String, Object> retrievalParams) {
            this.retrievalParams = retrievalParams != null ? 
                new HashMap<>(retrievalParams) : null;
            return this;
        }
        
        public Builder retrievalParam(String key, Object value) {
            if (this.retrievalParams == null) {
                this.retrievalParams = new HashMap<>();
            }
            this.retrievalParams.put(key, value);
            return this;
        }
        
        public Builder userPreferences(Map<String, Object> userPreferences) {
            this.userPreferences = userPreferences != null ? 
                new HashMap<>(userPreferences) : null;
            return this;
        }
        
        public Builder userPreference(String key, Object value) {
            if (this.userPreferences == null) {
                this.userPreferences = new HashMap<>();
            }
            this.userPreferences.put(key, value);
            return this;
        }
        
        public Builder currentStep(String currentStep) {
            this.currentStep = currentStep;
            return this;
        }
        
        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder currentTime(LocalDateTime currentTime) {
            this.currentTime = currentTime;
            return this;
        }
        
        public Builder processingStatus(String processingStatus) {
            this.processingStatus = processingStatus;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public Builder debugMode(boolean debugMode) {
            this.debugMode = debugMode;
            return this;
        }
        
        public Builder extensionProperties(Map<String, Object> extensionProperties) {
            this.extensionProperties = extensionProperties != null ? 
                new HashMap<>(extensionProperties) : null;
            return this;
        }
        
        public Builder extensionProperty(String key, Object value) {
            if (this.extensionProperties == null) {
                this.extensionProperties = new HashMap<>();
            }
            this.extensionProperties.put(key, value);
            return this;
        }
        
        /**
         * 验证构建参数
         */
        private void validate() {
            if (sessionId == null || sessionId.trim().isEmpty()) {
                throw new IllegalArgumentException("SessionId不能为空");
            }
            if (userId == null || userId.trim().isEmpty()) {
                throw new IllegalArgumentException("UserId不能为空");
            }
            if (queryId == null || queryId.trim().isEmpty()) {
                throw new IllegalArgumentException("QueryId不能为空");
            }
        }
        
        /**
         * 构建AgentContext实例
         */
        public AgentContext build() {
            validate();
            return new AgentContext(this);
        }
        
        /**
         * 构建AgentContext实例，但不进行验证（用于测试或特殊场景）
         */
        public AgentContext buildWithoutValidation() {
            return new AgentContext(this);
        }
        
        /**
         * 重置Builder状态
         */
        public Builder reset() {
            this.sessionId = null;
            this.userId = null;
            this.queryId = null;
            this.currentQuery = null;
            this.queryIntent = null;
            this.conversationHistory = null;
            this.preferredSources = null;
            this.retrievalParams = null;
            this.userPreferences = null;
            this.currentStep = null;
            this.startTime = null;
            this.currentTime = null;
            this.processingStatus = null;
            this.errorMessage = null;
            this.debugMode = false;
            this.extensionProperties = null;
            return this;
        }
    }
} 