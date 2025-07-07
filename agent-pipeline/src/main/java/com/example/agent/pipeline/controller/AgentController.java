package com.example.agent.pipeline.controller;

import com.example.agent.pipeline.service.AgentPipelineService;
import com.example.agent.pipeline.service.AgentProcessingResult;
import com.example.agent.knowledge.strategy.VectorStoreStrategyImpl;
import com.example.agent.knowledge.strategy.SqlDatabaseStrategyImpl;
import com.example.agent.knowledge.strategy.ApiSourceStrategyImpl;
import com.example.agent.core.dto.AgentContext;
import com.example.agent.core.dto.KnowledgeChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent核心API控制器
 * 提供问答、知识检索等核心功能接口
 */
@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);
    
    @Autowired
    private AgentPipelineService agentPipelineService;
    
    @Autowired
    private VectorStoreStrategyImpl vectorStoreStrategy;
    
    @Autowired
    private SqlDatabaseStrategyImpl sqlDatabaseStrategy;
    
    @Autowired
    private ApiSourceStrategyImpl apiSourceStrategy;
    
    /**
     * 问答接口 - 基础版本
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        logger.info("接收到问答请求: {}", request.getMessage());
        
        try {
            // 参数验证
            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ChatResponse.error("消息内容不能为空")
                );
            }
            
            // 处理查询
            AgentProcessingResult result = agentPipelineService.process(
                request.getMessage(),
                request.getUserId() != null ? request.getUserId() : "anonymous",
                request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString()
            );
            
            // 构建响应
            ChatResponse response = new ChatResponse();
            response.setQueryId(result.getQueryId());
            response.setSuccessful(result.isSuccessful());
            response.setResponse(result.getResponse());
            response.setProcessingTime(result.getDuration());
            response.setKnowledgeChunksCount(result.getKnowledgeChunksCount());
            response.setTimestamp(System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("问答处理异常", e);
            return ResponseEntity.internalServerError().body(
                ChatResponse.error("内部服务错误: " + e.getMessage())
            );
        }
    }
    
    /**
     * 问答接口 - 高级版本（支持更多参数）
     */
    @PostMapping("/chat/advanced")
    public ResponseEntity<ChatResponse> chatAdvanced(@RequestBody AdvancedChatRequest request) {
        logger.info("接收到高级问答请求: {}", request.getMessage());
        
        try {
            // 参数验证
            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ChatResponse.error("消息内容不能为空")
                );
            }
            
            // 构建处理选项
            Map<String, Object> options = new HashMap<>();
            if (request.getOptions() != null) {
                options.putAll(request.getOptions());
            }
            
            // 设置用户偏好
            if (request.getUserPreferences() != null) {
                options.put("userPreferences", request.getUserPreferences());
            }
            
            // 设置检索参数
            if (request.getRetrievalParams() != null) {
                options.put("retrievalParams", request.getRetrievalParams());
            }
            
            // 设置对话历史
            if (request.getConversationHistory() != null) {
                options.put("conversationHistory", request.getConversationHistory());
            }
            
            // 处理查询
            AgentProcessingResult result = agentPipelineService.process(
                request.getMessage(),
                request.getUserId() != null ? request.getUserId() : "anonymous",
                request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString(),
                request.getChainType() != null ? request.getChainType() : "standard",
                options
            );
            
            // 构建响应
            ChatResponse response = new ChatResponse();
            response.setQueryId(result.getQueryId());
            response.setSuccessful(result.isSuccessful());
            response.setResponse(result.getResponse());
            response.setProcessingTime(result.getDuration());
            response.setKnowledgeChunksCount(result.getKnowledgeChunksCount());
            response.setTimestamp(System.currentTimeMillis());
            response.setChainType(request.getChainType());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("高级问答处理异常", e);
            return ResponseEntity.internalServerError().body(
                ChatResponse.error("内部服务错误: " + e.getMessage())
            );
        }
    }
    
    /**
     * 知识检索接口 - 向量检索
     */
    @PostMapping("/knowledge/vector")
    public ResponseEntity<KnowledgeResponse> retrieveFromVector(@RequestBody KnowledgeRequest request) {
        logger.info("接收到向量检索请求: {}", request.getQuery());
        
        try {
            // 参数验证
            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    KnowledgeResponse.error("查询内容不能为空")
                );
            }
            
            // 创建上下文
            AgentContext context = new AgentContext(
                request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString(),
                request.getUserId() != null ? request.getUserId() : "anonymous",
                UUID.randomUUID().toString(),
                request.getQuery()
            );
            
            // 设置检索参数
            if (request.getRetrievalParams() != null) {
                context.setRetrievalParams(request.getRetrievalParams());
            }
            
            // 执行向量检索
            List<KnowledgeChunk> chunks = vectorStoreStrategy.retrieve(context);
            
            // 构建响应
            KnowledgeResponse response = new KnowledgeResponse();
            response.setQueryId(context.getQueryId());
            response.setSuccessful(true);
            response.setKnowledgeChunks(chunks);
            response.setChunksCount(chunks.size());
            response.setRetrievalType("vector");
            response.setTimestamp(System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("向量检索异常", e);
            return ResponseEntity.internalServerError().body(
                KnowledgeResponse.error("向量检索失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 知识检索接口 - 数据库检索
     */
    @PostMapping("/knowledge/database")
    public ResponseEntity<KnowledgeResponse> retrieveFromDatabase(@RequestBody KnowledgeRequest request) {
        logger.info("接收到数据库检索请求: {}", request.getQuery());
        
        try {
            // 参数验证
            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    KnowledgeResponse.error("查询内容不能为空")
                );
            }
            
            // 创建上下文
            AgentContext context = new AgentContext(
                request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString(),
                request.getUserId() != null ? request.getUserId() : "anonymous",
                UUID.randomUUID().toString(),
                request.getQuery()
            );
            
            // 设置检索参数
            if (request.getRetrievalParams() != null) {
                context.setRetrievalParams(request.getRetrievalParams());
            }
            
            // 执行数据库检索
            List<KnowledgeChunk> chunks = sqlDatabaseStrategy.retrieve(context);
            
            // 构建响应
            KnowledgeResponse response = new KnowledgeResponse();
            response.setQueryId(context.getQueryId());
            response.setSuccessful(true);
            response.setKnowledgeChunks(chunks);
            response.setChunksCount(chunks.size());
            response.setRetrievalType("database");
            response.setTimestamp(System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("数据库检索异常", e);
            return ResponseEntity.internalServerError().body(
                KnowledgeResponse.error("数据库检索失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 知识检索接口 - API检索
     */
    @PostMapping("/knowledge/api")
    public ResponseEntity<KnowledgeResponse> retrieveFromApi(@RequestBody KnowledgeRequest request) {
        logger.info("接收到API检索请求: {}", request.getQuery());
        
        try {
            // 参数验证
            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    KnowledgeResponse.error("查询内容不能为空")
                );
            }
            
            // 创建上下文
            AgentContext context = new AgentContext(
                request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString(),
                request.getUserId() != null ? request.getUserId() : "anonymous",
                UUID.randomUUID().toString(),
                request.getQuery()
            );
            
            // 设置检索参数
            if (request.getRetrievalParams() != null) {
                context.setRetrievalParams(request.getRetrievalParams());
            }
            
            // 执行API检索
            List<KnowledgeChunk> chunks = apiSourceStrategy.retrieve(context);
            
            // 构建响应
            KnowledgeResponse response = new KnowledgeResponse();
            response.setQueryId(context.getQueryId());
            response.setSuccessful(true);
            response.setKnowledgeChunks(chunks);
            response.setChunksCount(chunks.size());
            response.setRetrievalType("api");
            response.setTimestamp(System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("API检索异常", e);
            return ResponseEntity.internalServerError().body(
                KnowledgeResponse.error("API检索失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 系统状态接口
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "agent-pipeline");
        status.put("version", "1.0.0");
        status.put("timestamp", System.currentTimeMillis());
        status.put("uptime", System.currentTimeMillis());
        
        // 获取处理链信息
        status.put("availableChains", agentPipelineService.getAvailableChainTypes());
        status.put("executionStatistics", agentPipelineService.getExecutionStatistics());
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * 处理链信息接口
     */
    @GetMapping("/chains")
    public ResponseEntity<Map<String, Object>> getChains() {
        Map<String, Object> chains = new HashMap<>();
        
        for (String chainType : agentPipelineService.getAvailableChainTypes()) {
            chains.put(chainType, agentPipelineService.getChainInfo(chainType));
        }
        
        return ResponseEntity.ok(chains);
    }
    
    /**
     * 重置统计信息接口
     */
    @PostMapping("/statistics/reset")
    public ResponseEntity<Map<String, Object>> resetStatistics() {
        agentPipelineService.resetStatistics();
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "统计信息已重置");
        result.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(result);
    }
    
    // ===== 请求和响应类定义 =====
    
    /**
     * 基础聊天请求
     */
    public static class ChatRequest {
        private String message;
        private String userId;
        private String sessionId;
        
        // getters and setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }
    
    /**
     * 高级聊天请求
     */
    public static class AdvancedChatRequest extends ChatRequest {
        private String chainType;
        private Map<String, Object> options;
        private Map<String, Object> userPreferences;
        private Map<String, Object> retrievalParams;
        private List<String> conversationHistory;
        
        // getters and setters
        public String getChainType() { return chainType; }
        public void setChainType(String chainType) { this.chainType = chainType; }
        public Map<String, Object> getOptions() { return options; }
        public void setOptions(Map<String, Object> options) { this.options = options; }
        public Map<String, Object> getUserPreferences() { return userPreferences; }
        public void setUserPreferences(Map<String, Object> userPreferences) { this.userPreferences = userPreferences; }
        public Map<String, Object> getRetrievalParams() { return retrievalParams; }
        public void setRetrievalParams(Map<String, Object> retrievalParams) { this.retrievalParams = retrievalParams; }
        public List<String> getConversationHistory() { return conversationHistory; }
        public void setConversationHistory(List<String> conversationHistory) { this.conversationHistory = conversationHistory; }
    }
    
    /**
     * 知识检索请求
     */
    public static class KnowledgeRequest {
        private String query;
        private String userId;
        private String sessionId;
        private Map<String, Object> retrievalParams;
        
        // getters and setters
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public Map<String, Object> getRetrievalParams() { return retrievalParams; }
        public void setRetrievalParams(Map<String, Object> retrievalParams) { this.retrievalParams = retrievalParams; }
    }
    
    /**
     * 聊天响应
     */
    public static class ChatResponse {
        private String queryId;
        private boolean successful;
        private String response;
        private long processingTime;
        private int knowledgeChunksCount;
        private long timestamp;
        private String chainType;
        private String error;
        
        public static ChatResponse error(String error) {
            ChatResponse response = new ChatResponse();
            response.setSuccessful(false);
            response.setError(error);
            response.setTimestamp(System.currentTimeMillis());
            return response;
        }
        
        // getters and setters
        public String getQueryId() { return queryId; }
        public void setQueryId(String queryId) { this.queryId = queryId; }
        public boolean isSuccessful() { return successful; }
        public void setSuccessful(boolean successful) { this.successful = successful; }
        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
        public long getProcessingTime() { return processingTime; }
        public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }
        public int getKnowledgeChunksCount() { return knowledgeChunksCount; }
        public void setKnowledgeChunksCount(int knowledgeChunksCount) { this.knowledgeChunksCount = knowledgeChunksCount; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public String getChainType() { return chainType; }
        public void setChainType(String chainType) { this.chainType = chainType; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
    
    /**
     * 知识检索响应
     */
    public static class KnowledgeResponse {
        private String queryId;
        private boolean successful;
        private List<KnowledgeChunk> knowledgeChunks;
        private int chunksCount;
        private String retrievalType;
        private long timestamp;
        private String error;
        
        public static KnowledgeResponse error(String error) {
            KnowledgeResponse response = new KnowledgeResponse();
            response.setSuccessful(false);
            response.setError(error);
            response.setTimestamp(System.currentTimeMillis());
            return response;
        }
        
        // getters and setters
        public String getQueryId() { return queryId; }
        public void setQueryId(String queryId) { this.queryId = queryId; }
        public boolean isSuccessful() { return successful; }
        public void setSuccessful(boolean successful) { this.successful = successful; }
        public List<KnowledgeChunk> getKnowledgeChunks() { return knowledgeChunks; }
        public void setKnowledgeChunks(List<KnowledgeChunk> knowledgeChunks) { this.knowledgeChunks = knowledgeChunks; }
        public int getChunksCount() { return chunksCount; }
        public void setChunksCount(int chunksCount) { this.chunksCount = chunksCount; }
        public String getRetrievalType() { return retrievalType; }
        public void setRetrievalType(String retrievalType) { this.retrievalType = retrievalType; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
} 