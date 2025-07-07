package com.example.agent.pipeline.controller;

import com.example.agent.pipeline.service.AgentPipelineService;
import com.example.agent.pipeline.service.AgentProcessingResult;
import com.example.agent.pipeline.streaming.StreamingService;
import com.example.agent.pipeline.streaming.StreamingProgressListener;
import com.example.agent.pipeline.streaming.FluxStreamingService;
import com.example.agent.pipeline.streaming.FluxProgressListener;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final StreamingService streamingService;
    private final FluxStreamingService fluxStreamingService;
    private final AgentPipelineService agentPipelineService;

    @Value("${spring.ai.openai.chat.options.model}")
    private String modelName;

    public ChatController(ChatClient.Builder chatClientBuilder, 
                         StreamingService streamingService,
                         FluxStreamingService fluxStreamingService,
                         AgentPipelineService agentPipelineService) {
        this.chatClient = chatClientBuilder.build();
        this.streamingService = streamingService;
        this.fluxStreamingService = fluxStreamingService;
        this.agentPipelineService = agentPipelineService;
    }

    /**
     * 统一查询端点 - 自动选择流式或非流式响应
     * 根据Accept头或stream参数决定响应模式
     */
    @GetMapping("/ai/query")
    public Object unifiedQuery(@RequestParam(value = "message") String message,
                              @RequestParam(value = "sessionId", required = false) String sessionId,
                              @RequestParam(value = "stream", required = false) Boolean streamParam,
                              @RequestParam(value = "chainType", required = false) String chainType,
                              @RequestParam(value = "options", required = false) Map<String, Object> options,
                              HttpServletRequest request) {
        
        // 决定是否使用流式响应
        boolean useStreaming = shouldUseStreaming(request, streamParam);
        
        if (useStreaming) {
            return handleStreamingQuery(message, sessionId, chainType, options);
        } else {
            return handleRegularQuery(message, sessionId, chainType, options);
        }
    }
    
    /**
     * 统一查询端点 - POST版本
     */
    @PostMapping("/ai/query")
    public Object unifiedQueryPost(@RequestBody UnifiedQueryRequest request, 
                                   HttpServletRequest httpRequest) {
        
        // 决定是否使用流式响应
        boolean useStreaming = shouldUseStreaming(httpRequest, request.getStream());
        
        if (useStreaming) {
            return handleStreamingQueryFromRequest(request);
        } else {
            return handleRegularQueryFromRequest(request);
        }
    }
    
    /**
     * 处理流式查询
     */
    private SseEmitter handleStreamingQuery(String message, String sessionId, 
                                          String chainType, Map<String, Object> options) {
        // 生成会话ID（如果未提供）
        if (sessionId == null) {
            sessionId = "stream_" + UUID.randomUUID().toString();
        }
        
        final String finalSessionId = sessionId;
        final String finalUserId = "anonymous"; // 简化用户管理
        final String finalChainType = chainType != null ? chainType : "standard";
        final Map<String, Object> finalOptions = options != null ? options : new HashMap<>();
        
        // 创建SSE连接
        SseEmitter emitter = streamingService.createConnection(finalSessionId);
        
        // 创建进度监听器
        StreamingProgressListener progressListener = new StreamingProgressListener(streamingService, finalSessionId);
        
        // 立即开始异步处理查询
        agentPipelineService.processQueryAsync(message, finalUserId, finalSessionId, finalChainType, finalOptions, progressListener)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        progressListener.onError("Processing failed: " + throwable.getMessage());
                    } else {
                        progressListener.onCompleted("Query processed successfully");
                        // 发送最终结果
                        progressListener.onData("final_result", result);
                    }
                    
                    // 处理完成后关闭连接
                    try {
                        Thread.sleep(1000); // 给客户端一点时间接收最后的事件
                        streamingService.closeConnection(finalSessionId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
        
        return emitter;
    }
    
    /**
     * 处理非流式查询
     */
    private ResponseEntity<UnifiedQueryResponse> handleRegularQuery(String message, String sessionId, 
                                                                   String chainType, 
                                                                   Map<String, Object> options) {
        try {
            // 参数验证
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    UnifiedQueryResponse.error("消息内容不能为空")
                );
            }
            
            final String finalSessionId = sessionId != null ? sessionId : UUID.randomUUID().toString();
            final String finalUserId = "anonymous"; // 简化用户管理
            final String finalChainType = chainType != null ? chainType : "standard";
            final Map<String, Object> finalOptions = options != null ? options : new HashMap<>();
            
            // 处理查询
            AgentProcessingResult result = agentPipelineService.process(
                message, finalUserId, finalSessionId, finalChainType, finalOptions
            );
            
            // 构建响应
            UnifiedQueryResponse response = new UnifiedQueryResponse();
            response.setQueryId(result.getQueryId());
            response.setSuccessful(result.isSuccessful());
            response.setResponse(result.getResponse());
            response.setProcessingTime(result.getDuration());
            response.setKnowledgeChunksCount(result.getKnowledgeChunksCount());
            response.setTimestamp(System.currentTimeMillis());
            response.setSessionId(finalSessionId);
            response.setUserId(finalUserId);
            response.setChainType(finalChainType);
            response.setStreaming(false);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                UnifiedQueryResponse.error("内部服务错误: " + e.getMessage())
            );
        }
    }
    
    /**
     * 从请求对象处理流式查询
     */
    private SseEmitter handleStreamingQueryFromRequest(UnifiedQueryRequest request) {
        return handleStreamingQuery(
            request.getMessage(),
            request.getSessionId(),
            request.getChainType(),
            request.getOptions()
        );
    }
    
    /**
     * 从请求对象处理非流式查询
     */
    private ResponseEntity<UnifiedQueryResponse> handleRegularQueryFromRequest(UnifiedQueryRequest request) {
        return handleRegularQuery(
            request.getMessage(),
            request.getSessionId(),
            request.getChainType(), 
            request.getOptions()
        );
    }
    
    /**
     * 判断是否应该使用流式响应
     */
    private boolean shouldUseStreaming(HttpServletRequest request, Boolean streamParam) {
        // 优先检查显式的stream参数
        if (streamParam != null) {
            return streamParam;
        }
        
        // 检查Accept头
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null) {
            // 如果Accept头包含text/event-stream，使用流式响应
            if (acceptHeader.contains("text/event-stream")) {
                return true;
            }
            // 如果Accept头只包含application/json，使用非流式响应
            if (acceptHeader.contains("application/json") && !acceptHeader.contains("text/event-stream")) {
                return false;
            }
        }
        
        // 默认使用非流式响应
        return false;
    }

    @GetMapping("/ai/chat")
    public String chat(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return chatClient.prompt()
                .options(OpenAiChatOptions.builder().model(modelName).build())
                .user(message).call().content();
    }
    
    /**
     * 合并的SSE流式处理端点
     * 一次调用完成连接建立和查询处理
     */
    @GetMapping(value = "/ai/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamQuery(@RequestParam(value = "message") String message,
                                  @RequestParam(value = "sessionId", required = false) String sessionId,
                                  @RequestParam(value = "options", required = false) Map<String, Object> options) {
        
        // 生成会话ID（如果未提供）
        if (sessionId == null) {
            sessionId = "stream_" + UUID.randomUUID().toString();
        }
        
        final String finalSessionId = sessionId;
        
        // 创建SSE连接
        SseEmitter emitter = streamingService.createConnection(finalSessionId);
        
        // 创建进度监听器
        StreamingProgressListener progressListener = new StreamingProgressListener(streamingService, finalSessionId);
        
        // 立即开始异步处理查询
        agentPipelineService.processQueryAsync(message, progressListener)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        progressListener.onError("Processing failed: " + throwable.getMessage());
                    } else {
                        progressListener.onCompleted("Query processed successfully");
                        // 发送最终结果
                        progressListener.onData("final_result", result);
                    }
                    
                    // 处理完成后关闭连接
                    try {
                        Thread.sleep(1000); // 给客户端一点时间接收最后的事件
                        streamingService.closeConnection(finalSessionId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
        
        return emitter;
    }
    
    /**
     * 合并的SSE流式处理端点（POST版本）
     * 支持复杂的请求体参数
     */
    @PostMapping(value = "/ai/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamQueryPost(@RequestBody StreamQueryRequest request) {
        
        // 生成会话ID（如果未提供）
        String sessionId = request.getSessionId();
        if (sessionId == null) {
            sessionId = "stream_" + UUID.randomUUID().toString();
        }
        
        final String finalSessionId = sessionId;
        
        // 创建SSE连接
        SseEmitter emitter = streamingService.createConnection(finalSessionId);
        
        // 创建进度监听器
        StreamingProgressListener progressListener = new StreamingProgressListener(streamingService, finalSessionId);
        
        // 立即开始异步处理查询
        agentPipelineService.processQueryAsync(request.getMessage(), progressListener)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        progressListener.onError("Processing failed: " + throwable.getMessage());
                    } else {
                        progressListener.onCompleted("Query processed successfully");
                        // 发送最终结果
                        progressListener.onData("final_result", result);
                    }
                    
                    // 根据配置决定是否自动关闭连接
                    if (request.isAutoClose()) {
                        try {
                            Thread.sleep(1000); // 给客户端一点时间接收最后的事件
                            streamingService.closeConnection(finalSessionId);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
        
        return emitter;
    }

    /**
     * 创建SSE连接（保留兼容性）
     */
    @GetMapping("/ai/stream/connect")
    public SseEmitter streamConnect(@RequestParam(value = "sessionId", required = false) String sessionId) {
        return streamingService.createConnection(sessionId);
    }
    
    /**
     * 流式处理查询（保留兼容性）
     */
    @GetMapping("/ai/stream/chat")
    public void streamChat(@RequestParam(value = "message") String message,
                          @RequestParam(value = "sessionId") String sessionId) {
        // 检查会话是否存在
        if (!streamingService.hasConnection(sessionId)) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        
        // 创建进度监听器
        StreamingProgressListener progressListener = new StreamingProgressListener(streamingService, sessionId);
        
        // 异步处理查询
        agentPipelineService.processQueryAsync(message, progressListener)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        progressListener.onError("Processing failed: " + throwable.getMessage());
                    } else {
                        progressListener.onCompleted("Query processed successfully");
                        // 发送最终结果
                        progressListener.onData("final_result", result);
                    }
                });
    }
    
    /**
     * 流式处理查询（POST版本，支持复杂参数，保留兼容性）
     */
    @PostMapping("/ai/stream/chat")
    public void streamChatPost(@RequestBody StreamChatRequest request) {
        // 检查会话是否存在
        if (!streamingService.hasConnection(request.getSessionId())) {
            throw new IllegalArgumentException("Session not found: " + request.getSessionId());
        }
        
        // 创建进度监听器
        StreamingProgressListener progressListener = new StreamingProgressListener(streamingService, request.getSessionId());
        
        // 异步处理查询
        agentPipelineService.processQueryAsync(request.getMessage(), progressListener)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        progressListener.onError("Processing failed: " + throwable.getMessage());
                    } else {
                        progressListener.onCompleted("Query processed successfully");
                        progressListener.onData("final_result", result);
                    }
                });
    }
    
    /**
     * 获取流式处理状态
     */
    @GetMapping("/ai/stream/status")
    public Map<String, Object> getStreamingStatus(@RequestParam(value = "sessionId") String sessionId) {
        Map<String, Object> status = new HashMap<>();
        status.put("sessionId", sessionId);
        status.put("connected", streamingService.hasConnection(sessionId));
        status.put("activeConnections", streamingService.getActiveConnectionCount());
        status.put("timestamp", System.currentTimeMillis());
        return status;
    }
    
    /**
     * 关闭流式连接
     */
    @PostMapping("/ai/stream/close")
    public Map<String, Object> closeStreamingConnection(@RequestParam(value = "sessionId") String sessionId) {
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        
        if (streamingService.hasConnection(sessionId)) {
            streamingService.closeConnection(sessionId);
            result.put("closed", true);
            result.put("message", "Connection closed successfully");
        } else {
            result.put("closed", false);
            result.put("message", "Session not found or already closed");
        }
        
        return result;
    }
    
    /**
     * 统一查询请求类
     */
    public static class UnifiedQueryRequest {
        private String message;
        private String sessionId;
        private String userId;
        private String chainType;
        private Map<String, Object> options;
        private Boolean stream; // 显式控制流式/非流式
        private Map<String, Object> userPreferences;
        private Map<String, Object> retrievalParams;
        private List<String> conversationHistory;
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getChainType() { return chainType; }
        public void setChainType(String chainType) { this.chainType = chainType; }
        
        public Map<String, Object> getOptions() { return options; }
        public void setOptions(Map<String, Object> options) { this.options = options; }
        
        public Boolean getStream() { return stream; }
        public void setStream(Boolean stream) { this.stream = stream; }
        
        public Map<String, Object> getUserPreferences() { return userPreferences; }
        public void setUserPreferences(Map<String, Object> userPreferences) { this.userPreferences = userPreferences; }
        
        public Map<String, Object> getRetrievalParams() { return retrievalParams; }
        public void setRetrievalParams(Map<String, Object> retrievalParams) { this.retrievalParams = retrievalParams; }
        
        public List<String> getConversationHistory() { return conversationHistory; }
        public void setConversationHistory(List<String> conversationHistory) { this.conversationHistory = conversationHistory; }
    }
    
    /**
     * 统一查询响应类
     */
    public static class UnifiedQueryResponse {
        private String queryId;
        private boolean successful;
        private String response;
        private long processingTime;
        private int knowledgeChunksCount;
        private long timestamp;
        private String sessionId;
        private String userId;
        private String chainType;
        private boolean streaming;
        private String error;
        
        public static UnifiedQueryResponse error(String error) {
            UnifiedQueryResponse response = new UnifiedQueryResponse();
            response.setSuccessful(false);
            response.setError(error);
            response.setTimestamp(System.currentTimeMillis());
            return response;
        }
        
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
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getChainType() { return chainType; }
        public void setChainType(String chainType) { this.chainType = chainType; }
        
        public boolean isStreaming() { return streaming; }
        public void setStreaming(boolean streaming) { this.streaming = streaming; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
    
    /**
     * 合并的流式查询请求类（保留兼容性）
     */
    public static class StreamQueryRequest {
        private String message;
        private String sessionId;
        private Map<String, Object> options;
        private boolean autoClose = true; // 默认自动关闭连接
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }
        
        public Map<String, Object> getOptions() {
            return options;
        }
        
        public void setOptions(Map<String, Object> options) {
            this.options = options;
        }
        
        public boolean isAutoClose() {
            return autoClose;
        }
        
        public void setAutoClose(boolean autoClose) {
            this.autoClose = autoClose;
        }
    }
    
    /**
     * 流式聊天请求类（保留兼容性）
     */
    public static class StreamChatRequest {
        private String message;
        private String sessionId;
        private Map<String, Object> options;
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }
        
        public Map<String, Object> getOptions() {
            return options;
        }
        
        public void setOptions(Map<String, Object> options) {
            this.options = options;
        }
    }
    
    // ========================= Flux 响应式流端点 =========================
    
    /**
     * Flux流式查询端点 - JSON格式
     */
    @GetMapping(value = "/ai/flux/query", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<String> fluxQueryJson(@RequestParam(value = "message") String message,
                                     @RequestParam(value = "sessionId", required = false) String sessionId,
                                     @RequestParam(value = "userId", required = false) String userId,
                                     @RequestParam(value = "chainType", required = false) String chainType,
                                     @RequestParam(value = "options", required = false) Map<String, Object> options) {
        
        // 生成会话ID（如果未提供）
        if (sessionId == null) {
            sessionId = "flux_" + UUID.randomUUID().toString();
        }
        
        final String finalSessionId = sessionId;
        final String finalUserId = userId != null ? userId : "anonymous";
        final String finalChainType = chainType != null ? chainType : "standard";
        final Map<String, Object> finalOptions = options != null ? options : new HashMap<>();
        
        // 创建Flux监听器
        FluxProgressListener progressListener = fluxStreamingService.createListener(finalSessionId);
        
        // 启动异步处理
        agentPipelineService.processQueryAsync(message, finalUserId, finalSessionId, finalChainType, finalOptions, progressListener)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        progressListener.onError("Processing failed: " + throwable.getMessage());
                    } else {
                        progressListener.onData("final_result", result);
                        progressListener.onCompleted("Query processed successfully");
                    }
                });
        
        return fluxStreamingService.getJsonEventStream(finalSessionId);
    }
    
    /**
     * Flux流式查询端点 - SSE格式
     */
    @GetMapping(value = "/ai/flux/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> fluxQuerySse(@RequestParam(value = "message") String message,
                                                     @RequestParam(value = "sessionId", required = false) String sessionId,
                                                     @RequestParam(value = "userId", required = false) String userId,
                                                     @RequestParam(value = "chainType", required = false) String chainType,
                                                     @RequestParam(value = "options", required = false) Map<String, Object> options) {
        
        // 生成会话ID（如果未提供）
        if (sessionId == null) {
            sessionId = "flux_" + UUID.randomUUID().toString();
        }
        
        final String finalSessionId = sessionId;
        final String finalUserId = userId != null ? userId : "anonymous";
        final String finalChainType = chainType != null ? chainType : "standard";
        final Map<String, Object> finalOptions = options != null ? options : new HashMap<>();
        
        // 创建Flux监听器
        FluxProgressListener progressListener = fluxStreamingService.createListener(finalSessionId);
        
        // 启动异步处理
        agentPipelineService.processQueryAsync(message, finalUserId, finalSessionId, finalChainType, finalOptions, progressListener)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        progressListener.onError("Processing failed: " + throwable.getMessage());
                    } else {
                        progressListener.onData("final_result", result);
                        progressListener.onCompleted("Query processed successfully");
                    }
                });
        
        return fluxStreamingService.getSseEventStreamWithHeartbeat(finalSessionId);
    }
    
    /**
     * Flux流式查询端点 - POST版本（JSON格式）
     */
    @PostMapping(value = "/ai/flux/query", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<String> fluxQueryJsonPost(@RequestBody FluxQueryRequest request) {
        
        // 生成会话ID（如果未提供）
        String sessionId = request.getSessionId();
        if (sessionId == null) {
            sessionId = "flux_" + UUID.randomUUID().toString();
        }
        
        final String finalSessionId = sessionId;
        
        // 创建Flux监听器
        FluxProgressListener progressListener = fluxStreamingService.createListener(finalSessionId);
        
        // 启动异步处理
        agentPipelineService.processQueryAsync(
                request.getMessage(), 
                request.getUserId() != null ? request.getUserId() : "anonymous",
                finalSessionId,
                request.getChainType() != null ? request.getChainType() : "standard",
                request.getOptions() != null ? request.getOptions() : new HashMap<>(),
                progressListener)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        progressListener.onError("Processing failed: " + throwable.getMessage());
                    } else {
                        progressListener.onData("final_result", result);
                        progressListener.onCompleted("Query processed successfully");
                    }
                });
        
        return fluxStreamingService.getJsonEventStream(finalSessionId);
    }
    
    /**
     * Flux流式查询端点 - POST版本（SSE格式）
     */
    @PostMapping(value = "/ai/flux/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> fluxQuerySsePost(@RequestBody FluxQueryRequest request) {
        
        // 生成会话ID（如果未提供）
        String sessionId = request.getSessionId();
        if (sessionId == null) {
            sessionId = "flux_" + UUID.randomUUID().toString();
        }
        
        final String finalSessionId = sessionId;
        
        // 创建Flux监听器
        FluxProgressListener progressListener = fluxStreamingService.createListener(finalSessionId);
        
        // 启动异步处理
        agentPipelineService.processQueryAsync(
                request.getMessage(), 
                request.getUserId() != null ? request.getUserId() : "anonymous",
                finalSessionId,
                request.getChainType() != null ? request.getChainType() : "standard",
                request.getOptions() != null ? request.getOptions() : new HashMap<>(),
                progressListener)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        progressListener.onError("Processing failed: " + throwable.getMessage());
                    } else {
                        progressListener.onData("final_result", result);
                        progressListener.onCompleted("Query processed successfully");
                    }
                });
        
        return fluxStreamingService.getSseEventStreamWithHeartbeat(finalSessionId);
    }
    
    /**
     * 获取Flux流式处理状态
     */
    @GetMapping("/ai/flux/status")
    public Mono<Map<String, Object>> getFluxStreamingStatus(@RequestParam(value = "sessionId", required = false) String sessionId) {
        return Mono.fromCallable(() -> {
            Map<String, Object> status = new HashMap<>();
            
            if (sessionId != null) {
                status.put("sessionId", sessionId);
                status.put("hasActiveListener", fluxStreamingService.hasActiveListener(sessionId));
            }
            
            status.putAll(fluxStreamingService.getServiceStatus());
            return status;
        });
    }
    
    /**
     * 关闭Flux监听器
     */
    @PostMapping("/ai/flux/close")
    public Mono<Map<String, Object>> closeFluxListener(@RequestParam(value = "sessionId") String sessionId) {
        return fluxStreamingService.closeListener(sessionId)
                .then(Mono.fromCallable(() -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("message", "Flux listener closed successfully");
                    result.put("sessionId", sessionId);
                    result.put("timestamp", System.currentTimeMillis());
                    return result;
                }))
                .onErrorResume(error -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", false);
                    result.put("message", "Failed to close flux listener: " + error.getMessage());
                    result.put("sessionId", sessionId);
                    result.put("timestamp", System.currentTimeMillis());
                    return Mono.just(result);
                });
    }
    
    /**
     * Flux查询请求对象
     */
    public static class FluxQueryRequest {
        private String message;
        private String sessionId;
        private String userId;
        private String chainType;
        private Map<String, Object> options;
        private Map<String, Object> userPreferences;
        private Map<String, Object> retrievalParams;
        private List<String> conversationHistory;
        
        // Getters and setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
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
}
