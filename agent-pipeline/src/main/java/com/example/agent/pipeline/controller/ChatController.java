package com.example.agent.pipeline.controller;

import com.example.agent.pipeline.service.AgentPipelineService;
import com.example.agent.pipeline.streaming.StreamingService;
import com.example.agent.pipeline.streaming.StreamingProgressListener;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final StreamingService streamingService;
    private final AgentPipelineService agentPipelineService;

    @Value("${spring.ai.openai.chat.options.model}")
    private String modelName;

    public ChatController(ChatClient.Builder chatClientBuilder, 
                         StreamingService streamingService,
                         AgentPipelineService agentPipelineService) {
        this.chatClient = chatClientBuilder.build();
        this.streamingService = streamingService;
        this.agentPipelineService = agentPipelineService;
    }

    @GetMapping("/ai/chat")
    public String chat(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return chatClient.prompt()
                .options(OpenAiChatOptions.builder().model(modelName).build())
                .user(message).call().content();
    }
    
    /**
     * 创建SSE连接
     */
    @GetMapping("/ai/stream/connect")
    public SseEmitter streamConnect(@RequestParam(value = "sessionId", required = false) String sessionId) {
        return streamingService.createConnection(sessionId);
    }
    
    /**
     * 流式处理查询
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
     * 流式处理查询（POST版本，支持复杂参数）
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
     * 流式聊天请求类
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
}
