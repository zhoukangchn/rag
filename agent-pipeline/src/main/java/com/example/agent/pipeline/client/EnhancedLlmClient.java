package com.example.agent.pipeline.client;

import com.example.agent.pipeline.config.LlmClientConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 增强版LLM客户端
 * 集成超时、重试机制和多种模型API支持
 */
@Component
public class EnhancedLlmClient {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedLlmClient.class);
    
    @Value("${agent.llm.api.openai.endpoint:https://api.openai.com/v1/chat/completions}")
    private String openaiEndpoint;
    
    @Value("${agent.llm.api.openai.key:}")
    private String openaiApiKey;
    
    @Value("${agent.llm.api.claude.endpoint:https://api.anthropic.com/v1/messages}")
    private String claudeEndpoint;
    
    @Value("${agent.llm.api.claude.key:}")
    private String claudeApiKey;
    
    @Value("${agent.llm.api.local.endpoint:http://localhost:11434/api/chat}")
    private String localEndpoint;
    
    @Value("${agent.llm.default.model:gpt-3.5-turbo}")
    private String defaultModel;
    
    @Value("${agent.llm.default.temperature:0.7}")
    private double defaultTemperature;
    
    @Value("${agent.llm.default.max-tokens:2000}")
    private int defaultMaxTokens;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final LlmClientConfig clientConfig;
    private final Retry retrySpec;
    
    @Autowired
    public EnhancedLlmClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10485760))
                .build();
        this.objectMapper = new ObjectMapper();
        this.clientConfig = null;
        this.retrySpec = Retry.backoff(3, Duration.ofMillis(1000));
    }
    
    /**
     * 通用LLM调用接口
     */
    public Mono<String> callLlm(String systemPrompt, String userPrompt, Map<String, Object> options) {
        // 确定使用的模型和端点
        String model = (String) options.getOrDefault("model", defaultModel);
        LlmProvider provider = determineProvider(model);
        
        logger.info("调用LLM - 提供商: {}, 模型: {}", provider, model);
        
        return buildRequestBody(systemPrompt, userPrompt, options, provider)
                .flatMap(requestBody -> 
                    makeApiCall(requestBody, provider)
                            .timeout(Duration.ofMillis(60000))
                            .retryWhen(retrySpec)
                )
                .map(this::extractResponseContent)
                .doOnSuccess(response -> 
                    logger.debug("LLM调用成功 - 响应长度: {}", response.length())
                )
                .doOnError(error -> 
                    logger.error("LLM调用失败", error)
                );
    }
    
    /**
     * 流式LLM调用接口
     */
    public Mono<String> callLlmStreaming(String systemPrompt, String userPrompt, 
                                        Map<String, Object> options,
                                        StreamingCallback callback) {
        
        String model = (String) options.getOrDefault("model", defaultModel);
        LlmProvider provider = determineProvider(model);
        
        // 添加流式标记
        Map<String, Object> streamOptions = new HashMap<>(options);
        streamOptions.put("stream", true);
        
        logger.info("调用流式LLM - 提供商: {}, 模型: {}", provider, model);
        
        return buildRequestBody(systemPrompt, userPrompt, streamOptions, provider)
                .flatMap(requestBody -> 
                    makeStreamingApiCall(requestBody, provider, callback)
                            .timeout(Duration.ofMillis(60000))
                            .retryWhen(retrySpec)
                )
                .doOnSuccess(response -> 
                    logger.debug("流式LLM调用完成 - 响应长度: {}", response.length())
                )
                .doOnError(error -> 
                    logger.error("流式LLM调用失败", error)
                );
    }
    
    /**
     * 确定LLM提供商
     */
    private LlmProvider determineProvider(String model) {
        if (model.startsWith("gpt-") || model.startsWith("text-davinci")) {
            return LlmProvider.OPENAI;
        } else if (model.startsWith("claude-")) {
            return LlmProvider.CLAUDE;
        } else {
            return LlmProvider.LOCAL;
        }
    }
    
    /**
     * 构建请求体
     */
    private Mono<Map<String, Object>> buildRequestBody(String systemPrompt, String userPrompt, 
                                                      Map<String, Object> options, 
                                                      LlmProvider provider) {
        return Mono.fromCallable(() -> {
            Map<String, Object> requestBody = new HashMap<>();
            
            // 通用参数
            requestBody.put("model", options.getOrDefault("model", defaultModel));
            requestBody.put("max_tokens", options.getOrDefault("maxTokens", defaultMaxTokens));
            requestBody.put("temperature", options.getOrDefault("temperature", defaultTemperature));
            
            // 根据提供商构建消息格式
            switch (provider) {
                case OPENAI:
                    requestBody.putAll(buildOpenAiRequest(systemPrompt, userPrompt, options));
                    break;
                case CLAUDE:
                    requestBody.putAll(buildClaudeRequest(systemPrompt, userPrompt, options));
                    break;
                case LOCAL:
                    requestBody.putAll(buildLocalRequest(systemPrompt, userPrompt, options));
                    break;
            }
            
            return requestBody;
        });
    }
    
    /**
     * 构建OpenAI请求格式
     */
    private Map<String, Object> buildOpenAiRequest(String systemPrompt, String userPrompt, 
                                                  Map<String, Object> options) {
        Map<String, Object> request = new HashMap<>();
        
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        
        request.put("messages", new Object[]{systemMessage, userMessage});
        
        // 流式处理标记
        if (options.containsKey("stream")) {
            request.put("stream", options.get("stream"));
        }
        
        return request;
    }
    
    /**
     * 构建Claude请求格式
     */
    private Map<String, Object> buildClaudeRequest(String systemPrompt, String userPrompt, 
                                                  Map<String, Object> options) {
        Map<String, Object> request = new HashMap<>();
        
        request.put("system", systemPrompt);
        
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        
        request.put("messages", new Object[]{userMessage});
        
        return request;
    }
    
    /**
     * 构建本地模型请求格式
     */
    private Map<String, Object> buildLocalRequest(String systemPrompt, String userPrompt, 
                                                 Map<String, Object> options) {
        Map<String, Object> request = new HashMap<>();
        
        // 简单的提示组合
        String prompt = systemPrompt + "\n\n" + userPrompt;
        request.put("prompt", prompt);
        
        return request;
    }
    
    /**
     * 执行API调用
     */
    private Mono<String> makeApiCall(Map<String, Object> requestBody, LlmProvider provider) {
        String endpoint = getEndpoint(provider);
        WebClient.RequestBodySpec requestSpec = webClient.post().uri(endpoint);
        
        // 设置认证头
        setAuthHeaders(requestSpec, provider);
        
        return requestSpec
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    logger.error("API调用失败 - 状态码: {}, 响应体: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                    return Mono.error(new RuntimeException("LLM API调用失败: " + ex.getMessage()));
                });
    }
    
    /**
     * 执行流式API调用
     */
    private Mono<String> makeStreamingApiCall(Map<String, Object> requestBody, 
                                             LlmProvider provider, 
                                             StreamingCallback callback) {
        String endpoint = getEndpoint(provider);
        WebClient.RequestBodySpec requestSpec = webClient.post().uri(endpoint);
        
        // 设置认证头
        setAuthHeaders(requestSpec, provider);
        
        return requestSpec
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(chunk -> {
                    try {
                        processStreamChunk(chunk, callback);
                    } catch (Exception e) {
                        logger.warn("处理流式数据块失败", e);
                    }
                })
                .collect(StringBuilder::new, StringBuilder::append)
                .map(StringBuilder::toString)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    logger.error("流式API调用失败 - 状态码: {}, 响应体: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                    return Mono.error(new RuntimeException("流式LLM API调用失败: " + ex.getMessage()));
                });
    }
    
    /**
     * 获取端点URL
     */
    private String getEndpoint(LlmProvider provider) {
        switch (provider) {
            case OPENAI:
                return openaiEndpoint;
            case CLAUDE:
                return claudeEndpoint;
            case LOCAL:
                return localEndpoint;
            default:
                return openaiEndpoint;
        }
    }
    
    /**
     * 设置认证头
     */
    private void setAuthHeaders(WebClient.RequestBodySpec requestSpec, LlmProvider provider) {
        switch (provider) {
            case OPENAI:
                if (openaiApiKey != null && !openaiApiKey.isEmpty()) {
                    requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey);
                }
                break;
            case CLAUDE:
                if (claudeApiKey != null && !claudeApiKey.isEmpty()) {
                    requestSpec.header("x-api-key", claudeApiKey);
                    requestSpec.header("anthropic-version", "2023-06-01");
                }
                break;
            case LOCAL:
                // 本地模型通常不需要认证
                break;
        }
    }
    
    /**
     * 处理流式数据块
     */
    private void processStreamChunk(String chunk, StreamingCallback callback) {
        if (callback == null) return;
        
        try {
            // 处理SSE格式数据
            if (chunk.startsWith("data: ")) {
                String jsonData = chunk.substring(6).trim();
                
                if ("[DONE]".equals(jsonData)) {
                    callback.onComplete();
                    return;
                }
                
                JsonNode jsonNode = objectMapper.readTree(jsonData);
                String content = extractStreamContent(jsonNode);
                
                if (content != null && !content.isEmpty()) {
                    callback.onChunk(content);
                }
            }
        } catch (Exception e) {
            logger.warn("处理流式数据块异常: {}", chunk, e);
            callback.onError(e);
        }
    }
    
    /**
     * 提取流式内容
     */
    private String extractStreamContent(JsonNode jsonNode) {
        JsonNode choices = jsonNode.get("choices");
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode choice = choices.get(0);
            JsonNode delta = choice.get("delta");
            
            if (delta != null && delta.has("content")) {
                return delta.get("content").asText();
            }
        }
        return null;
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
     * LLM提供商枚举
     */
    public enum LlmProvider {
        OPENAI, CLAUDE, LOCAL
    }
    
    /**
     * 流式回调接口
     */
    public interface StreamingCallback {
        void onChunk(String chunk);
        void onComplete();
        void onError(Throwable error);
    }
} 