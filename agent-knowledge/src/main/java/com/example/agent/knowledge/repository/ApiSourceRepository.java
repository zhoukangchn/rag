package com.example.agent.knowledge.repository;

import com.example.agent.core.dto.KnowledgeChunk;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 外部API访问仓库接口
 * 
 * @author agent
 */
public interface ApiSourceRepository {
    
    /**
     * 通过GET请求获取数据
     * 
     * @param url 请求URL
     * @param headers 请求头
     * @param queryParams 查询参数
     * @return 转换后的知识块列表
     */
    List<KnowledgeChunk> get(String url, Map<String, String> headers, Map<String, Object> queryParams);
    
    /**
     * 通过POST请求获取数据
     * 
     * @param url 请求URL
     * @param headers 请求头
     * @param requestBody 请求体
     * @return 转换后的知识块列表
     */
    List<KnowledgeChunk> post(String url, Map<String, String> headers, Object requestBody);
    
    /**
     * 通过PUT请求更新数据
     * 
     * @param url 请求URL
     * @param headers 请求头
     * @param requestBody 请求体
     * @return 更新结果
     */
    ApiResponse put(String url, Map<String, String> headers, Object requestBody);
    
    /**
     * 通过DELETE请求删除数据
     * 
     * @param url 请求URL
     * @param headers 请求头
     * @return 删除结果
     */
    ApiResponse delete(String url, Map<String, String> headers);
    
    /**
     * 异步GET请求
     * 
     * @param url 请求URL
     * @param headers 请求头
     * @param queryParams 查询参数
     * @return 异步结果
     */
    CompletableFuture<List<KnowledgeChunk>> getAsync(String url, Map<String, String> headers, Map<String, Object> queryParams);
    
    /**
     * 异步POST请求
     * 
     * @param url 请求URL
     * @param headers 请求头
     * @param requestBody 请求体
     * @return 异步结果
     */
    CompletableFuture<List<KnowledgeChunk>> postAsync(String url, Map<String, String> headers, Object requestBody);
    
    /**
     * 流式数据获取
     * 
     * @param url 请求URL
     * @param headers 请求头
     * @param callback 数据回调处理器
     */
    void streamData(String url, Map<String, String> headers, StreamDataCallback callback);
    
    /**
     * 批量API调用
     * 
     * @param requests 批量请求列表
     * @return 批量响应结果
     */
    List<ApiResponse> batchRequest(List<ApiRequest> requests);
    
    /**
     * 搜索外部知识源
     * 
     * @param sourceConfig 数据源配置
     * @param query 搜索查询
     * @param limit 结果限制
     * @return 搜索结果知识块列表
     */
    List<KnowledgeChunk> searchExternalSource(ApiSourceConfig sourceConfig, String query, int limit);
    
    /**
     * 获取数据源元信息
     * 
     * @param sourceConfig 数据源配置
     * @return 数据源元信息
     */
    ApiSourceMetadata getSourceMetadata(ApiSourceConfig sourceConfig);
    
    /**
     * 验证API连接
     * 
     * @param sourceConfig 数据源配置
     * @return 连接状态
     */
    boolean validateConnection(ApiSourceConfig sourceConfig);
    
    /**
     * 获取支持的API源类型
     * 
     * @return 支持的API源类型列表
     */
    List<String> getSupportedSourceTypes();
    
    /**
     * 获取API使用统计
     * 
     * @param sourceId 数据源ID
     * @return API使用统计信息
     */
    ApiUsageStats getUsageStats(String sourceId);
    
    /**
     * API请求封装类
     */
    class ApiRequest {
        private String method;
        private String url;
        private Map<String, String> headers;
        private Map<String, Object> queryParams;
        private Object body;
        private int timeout;
        
        public ApiRequest() {}
        
        public ApiRequest(String method, String url) {
            this.method = method;
            this.url = url;
            this.timeout = 30000; // 默认30秒超时
        }
        
        // Getters and Setters
        public String getMethod() {
            return method;
        }
        
        public void setMethod(String method) {
            this.method = method;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public Map<String, String> getHeaders() {
            return headers;
        }
        
        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }
        
        public Map<String, Object> getQueryParams() {
            return queryParams;
        }
        
        public void setQueryParams(Map<String, Object> queryParams) {
            this.queryParams = queryParams;
        }
        
        public Object getBody() {
            return body;
        }
        
        public void setBody(Object body) {
            this.body = body;
        }
        
        public int getTimeout() {
            return timeout;
        }
        
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }
    
    /**
     * API响应封装类
     */
    class ApiResponse {
        private int statusCode;
        private Map<String, String> headers;
        private Object body;
        private boolean success;
        private String errorMessage;
        private long responseTime;
        
        public ApiResponse() {}
        
        public ApiResponse(int statusCode, Object body) {
            this.statusCode = statusCode;
            this.body = body;
            this.success = statusCode >= 200 && statusCode < 300;
        }
        
        // Getters and Setters
        public int getStatusCode() {
            return statusCode;
        }
        
        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
            this.success = statusCode >= 200 && statusCode < 300;
        }
        
        public Map<String, String> getHeaders() {
            return headers;
        }
        
        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }
        
        public Object getBody() {
            return body;
        }
        
        public void setBody(Object body) {
            this.body = body;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        public long getResponseTime() {
            return responseTime;
        }
        
        public void setResponseTime(long responseTime) {
            this.responseTime = responseTime;
        }
    }
    
    /**
     * API数据源配置类
     */
    class ApiSourceConfig {
        private String sourceId;
        private String sourceType;
        private String baseUrl;
        private Map<String, String> defaultHeaders;
        private String authType;
        private Map<String, String> authConfig;
        private int rateLimitPerMinute;
        private int timeoutMs;
        
        public ApiSourceConfig() {}
        
        public ApiSourceConfig(String sourceId, String sourceType, String baseUrl) {
            this.sourceId = sourceId;
            this.sourceType = sourceType;
            this.baseUrl = baseUrl;
            this.timeoutMs = 30000;
            this.rateLimitPerMinute = 60;
        }
        
        // Getters and Setters
        public String getSourceId() {
            return sourceId;
        }
        
        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
        }
        
        public String getSourceType() {
            return sourceType;
        }
        
        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }
        
        public String getBaseUrl() {
            return baseUrl;
        }
        
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
        
        public Map<String, String> getDefaultHeaders() {
            return defaultHeaders;
        }
        
        public void setDefaultHeaders(Map<String, String> defaultHeaders) {
            this.defaultHeaders = defaultHeaders;
        }
        
        public String getAuthType() {
            return authType;
        }
        
        public void setAuthType(String authType) {
            this.authType = authType;
        }
        
        public Map<String, String> getAuthConfig() {
            return authConfig;
        }
        
        public void setAuthConfig(Map<String, String> authConfig) {
            this.authConfig = authConfig;
        }
        
        public int getRateLimitPerMinute() {
            return rateLimitPerMinute;
        }
        
        public void setRateLimitPerMinute(int rateLimitPerMinute) {
            this.rateLimitPerMinute = rateLimitPerMinute;
        }
        
        public int getTimeoutMs() {
            return timeoutMs;
        }
        
        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }
    
    /**
     * API数据源元信息类
     */
    class ApiSourceMetadata {
        private String sourceId;
        private String name;
        private String description;
        private String version;
        private List<String> supportedOperations;
        private Map<String, Object> capabilities;
        
        public ApiSourceMetadata() {}
        
        // Getters and Setters
        public String getSourceId() {
            return sourceId;
        }
        
        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
        
        public List<String> getSupportedOperations() {
            return supportedOperations;
        }
        
        public void setSupportedOperations(List<String> supportedOperations) {
            this.supportedOperations = supportedOperations;
        }
        
        public Map<String, Object> getCapabilities() {
            return capabilities;
        }
        
        public void setCapabilities(Map<String, Object> capabilities) {
            this.capabilities = capabilities;
        }
    }
    
    /**
     * API使用统计类
     */
    class ApiUsageStats {
        private String sourceId;
        private long totalRequests;
        private long successfulRequests;
        private long failedRequests;
        private double averageResponseTime;
        private long rateLimitHits;
        
        public ApiUsageStats() {}
        
        // Getters and Setters
        public String getSourceId() {
            return sourceId;
        }
        
        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
        }
        
        public long getTotalRequests() {
            return totalRequests;
        }
        
        public void setTotalRequests(long totalRequests) {
            this.totalRequests = totalRequests;
        }
        
        public long getSuccessfulRequests() {
            return successfulRequests;
        }
        
        public void setSuccessfulRequests(long successfulRequests) {
            this.successfulRequests = successfulRequests;
        }
        
        public long getFailedRequests() {
            return failedRequests;
        }
        
        public void setFailedRequests(long failedRequests) {
            this.failedRequests = failedRequests;
        }
        
        public double getAverageResponseTime() {
            return averageResponseTime;
        }
        
        public void setAverageResponseTime(double averageResponseTime) {
            this.averageResponseTime = averageResponseTime;
        }
        
        public long getRateLimitHits() {
            return rateLimitHits;
        }
        
        public void setRateLimitHits(long rateLimitHits) {
            this.rateLimitHits = rateLimitHits;
        }
    }
    
    /**
     * 流式数据回调接口
     */
    @FunctionalInterface
    interface StreamDataCallback {
        void onData(KnowledgeChunk chunk);
    }
} 