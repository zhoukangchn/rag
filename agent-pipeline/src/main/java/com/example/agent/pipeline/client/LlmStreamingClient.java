package com.example.agent.pipeline.client;

import com.example.agent.pipeline.streaming.ProgressListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * LLM流式客户端
 * 支持WebClient响应式处理和流式数据消费
 */
@Component
public class LlmStreamingClient {
    
    private static final Logger logger = LoggerFactory.getLogger(LlmStreamingClient.class);
    
    @Value("${agent.llm.stream.endpoint:http://localhost:8080/api/v1/chat/completions}")
    private String streamEndpoint;
    
    @Value("${agent.llm.stream.timeout:60000}")
    private int streamTimeoutMillis;
    
    @Value("${agent.llm.stream.buffer-size:1024}")
    private int bufferSize;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public LlmStreamingClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(bufferSize * 1024))
                .build();
        this.objectMapper = objectMapper;
    }
    
    /**
     * 流式调用LLM
     */
    public Mono<String> streamCall(String systemPrompt, String userPrompt, 
                                  Map<String, Object> options, 
                                  ProgressListener progressListener) {
        
        return buildRequestBody(systemPrompt, userPrompt, options)
                .flatMap(requestBody -> {
                    progressListener.onStepStarted("llm_streaming");
                    
                    return webClient.post()
                            .uri(streamEndpoint)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(requestBody)
                            .retrieve()
                            .bodyToFlux(String.class)
                            .timeout(Duration.ofMillis(streamTimeoutMillis))
                            .doOnNext(chunk -> {
                                try {
                                    processStreamChunk(chunk, progressListener);
                                } catch (Exception e) {
                                    logger.warn("处理流式数据块失败", e);
                                }
                            })
                            .doOnError(error -> {
                                logger.error("流式调用失败", error);
                                progressListener.onStepFailed("llm_streaming", error.getMessage());
                            })
                            .doOnComplete(() -> {
                                progressListener.onStepCompleted("llm_streaming");
                            })
                            .collect(StringBuilder::new, StringBuilder::append)
                            .map(StringBuilder::toString);
                })
                .onErrorResume(error -> {
                    logger.error("流式调用异常", error);
                    progressListener.onError("流式调用失败: " + error.getMessage());
                    return Mono.just("");
                });
    }
    
    /**
     * 构建请求体
     */
    private Mono<Map<String, Object>> buildRequestBody(String systemPrompt, String userPrompt, 
                                                      Map<String, Object> options) {
        return Mono.fromCallable(() -> {
            Map<String, Object> requestBody = new HashMap<>();
            
            // 设置模型参数
            requestBody.put("model", options.getOrDefault("model", "gpt-3.5-turbo"));
            requestBody.put("max_tokens", options.getOrDefault("maxTokens", 2000));
            requestBody.put("temperature", options.getOrDefault("temperature", 0.7));
            requestBody.put("stream", true);
            
            // 构建消息
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);
            
            requestBody.put("messages", new Object[]{systemMessage, userMessage});
            
            return requestBody;
        });
    }
    
    /**
     * 处理流式数据块
     */
    private void processStreamChunk(String chunk, ProgressListener progressListener) {
        try {
            // 处理SSE格式数据
            if (chunk.startsWith("data: ")) {
                String jsonData = chunk.substring(6).trim();
                
                // 检查是否为结束标记
                if ("[DONE]".equals(jsonData)) {
                    progressListener.onProgress("llm_streaming", 100.0, "流式处理完成");
                    return;
                }
                
                // 解析JSON数据
                JsonNode jsonNode = objectMapper.readTree(jsonData);
                
                // 提取内容
                JsonNode choices = jsonNode.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode choice = choices.get(0);
                    JsonNode delta = choice.get("delta");
                    
                    if (delta != null && delta.has("content")) {
                        String content = delta.get("content").asText();
                        
                        // 推送增量内容
                        progressListener.onData("llm_streaming", content);
                        
                        // 更新进度（基于内容长度估算）
                        double progress = Math.min(95.0, content.length() * 0.1);
                        progressListener.onProgress("llm_streaming", progress, "正在生成响应...");
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("处理流式数据块异常: {}", chunk, e);
        }
    }
    
    /**
     * 非流式调用（兼容性）
     */
    public Mono<String> regularCall(String systemPrompt, String userPrompt, 
                                   Map<String, Object> options) {
        
        return buildRequestBody(systemPrompt, userPrompt, options)
                .map(requestBody -> {
                    requestBody.put("stream", false);
                    return requestBody;
                })
                .flatMap(requestBody -> 
                    webClient.post()
                            .uri(streamEndpoint)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(requestBody)
                            .retrieve()
                            .bodyToMono(String.class)
                            .timeout(Duration.ofMillis(streamTimeoutMillis))
                )
                .map(this::extractResponseContent)
                .onErrorResume(error -> {
                    logger.error("常规调用异常", error);
                    return Mono.just("");
                });
    }
    
    /**
     * 提取响应内容
     */
    private String extractResponseContent(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode choices = jsonNode.get("choices");
            
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode choice = choices.get(0);
                JsonNode message = choice.get("message");
                
                if (message != null && message.has("content")) {
                    return message.get("content").asText();
                }
            }
        } catch (Exception e) {
            logger.error("提取响应内容失败", e);
        }
        
        return response;
    }
    
    /**
     * 批量流式调用
     */
    public Flux<String> streamBatch(String[] systemPrompts, String[] userPrompts, 
                                   Map<String, Object> options, 
                                   ProgressListener progressListener) {
        
        if (systemPrompts.length != userPrompts.length) {
            return Flux.error(new IllegalArgumentException("系统提示和用户提示数量不匹配"));
        }
        
        return Flux.range(0, systemPrompts.length)
                .flatMap(i -> {
                    progressListener.onProgress("batch_streaming", 
                            (double) i / systemPrompts.length * 100, 
                            String.format("处理批次 %d/%d", i + 1, systemPrompts.length));
                    
                    return streamCall(systemPrompts[i], userPrompts[i], options, progressListener);
                })
                .doOnComplete(() -> {
                    progressListener.onStepCompleted("batch_streaming");
                });
    }
} 