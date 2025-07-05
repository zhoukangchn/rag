package com.example.agent.pipeline.controller;

import com.example.agent.pipeline.service.AgentPipelineService;
import com.example.agent.pipeline.streaming.StreamingService;
import com.example.agent.pipeline.streaming.StreamingProgressListener;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
                    }
                });
    }
}
