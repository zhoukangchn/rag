package com.example.agent.pipeline.step;

import com.example.agent.core.dto.AgentContext;
import com.example.agent.pipeline.template.AbstractPipelineStep;
import com.example.agent.pipeline.client.LlmStreamingClient;
import com.example.agent.pipeline.streaming.ProgressListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 模型调用步骤
 * 负责调用LLM生成最终响应
 * 
 * @author agent
 */
@Component
public class ModelInvocationStep extends AbstractPipelineStep {
    
    private static final String STEP_NAME = "MODEL_INVOCATION";
    
    @Value("${agent.llm.endpoint:http://localhost:8080/api/v1/chat/completions}")
    private String llmEndpoint;
    
    @Value("${agent.llm.model:gpt-3.5-turbo}")
    private String defaultModel;
    
    @Value("${agent.llm.timeout:30000}")
    private int timeoutMillis;
    
    @Value("${agent.llm.max-tokens:2000}")
    private int maxTokens;
    
    @Value("${agent.llm.temperature:0.7}")
    private double temperature;
    
    private final WebClient webClient;
    private final ChatClient chatClient;
    private final LlmStreamingClient llmStreamingClient;
    
    @Autowired
    public ModelInvocationStep(ChatClient.Builder chatClientBuilder, LlmStreamingClient llmStreamingClient) {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
        this.chatClient = chatClientBuilder.build();
        this.llmStreamingClient = llmStreamingClient;
    }
    
    @Override
    protected boolean doExecute(AgentContext context) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 验证必要的上下文信息
            if (!validateContext(context, "query")) {
                return false;
            }
            
            // 获取构建的提示
            String systemPrompt = getSystemPrompt(context);
            String userPrompt = getUserPrompt(context);
            
            if (systemPrompt == null || userPrompt == null) {
                logger.error("未找到构建的提示，无法调用模型");
                return false;
            }
            
            // 检查是否为流式处理
            boolean isStreamingMode = isStreamingMode(context);
            String response;
            
            if (isStreamingMode) {
                // 使用流式客户端
                response = callLLMStreaming(context, systemPrompt, userPrompt);
            } else {
                // 使用Spring AI ChatClient调用LLM
                response = callLLM(context, systemPrompt, userPrompt);
            }
            
            if (response == null || response.isEmpty()) {
                logger.error("模型调用失败，未返回响应");
                return false;
            }
            
            // 后处理响应
            String processedResponse = postProcessResponse(context, response);
            
            // 保存响应到上下文
            context.addExtensionProperty("llmResponse", processedResponse);
            context.addExtensionProperty("responseLength", processedResponse.length());
            context.addExtensionProperty("streamingMode", isStreamingMode);
            
            // 记录执行时间
            long duration = System.currentTimeMillis() - startTime;
            recordExecutionTime(context, STEP_NAME, duration);
            
            logger.info("模型调用完成 - 查询ID: {}, 响应长度: {}, 耗时: {}ms, 流式模式: {}", 
                    context.getQueryId(), processedResponse.length(), duration, isStreamingMode);
            
            return true;
            
        } catch (Exception e) {
            logger.error("模型调用步骤执行失败", e);
            return false;
        }
    }
    
    /**
     * 获取系统提示
     */
    private String getSystemPrompt(AgentContext context) {
        Object systemPromptObj = context.getExtensionProperty("systemPrompt");
        return systemPromptObj instanceof String ? (String) systemPromptObj : null;
    }
    
    /**
     * 获取用户提示
     */
    private String getUserPrompt(AgentContext context) {
        Object userPromptObj = context.getExtensionProperty("userPrompt");
        return userPromptObj instanceof String ? (String) userPromptObj : null;
    }
    
    /**
     * 检查是否为流式处理模式
     */
    private boolean isStreamingMode(AgentContext context) {
        // 检查用户会话类型
        String sessionId = context.getSessionId();
        if (sessionId != null && sessionId.startsWith("stream")) {
            return true;
        }
        
        // 检查扩展属性
        Object streamingFlag = context.getExtensionProperty("streamingEnabled");
        if (streamingFlag instanceof Boolean) {
            return (Boolean) streamingFlag;
        }
        
        // 检查用户偏好
        Map<String, Object> userPreferences = context.getUserPreferences();
        if (userPreferences != null) {
            Object streamingPref = userPreferences.get("streamingEnabled");
            if (streamingPref instanceof Boolean) {
                return (Boolean) streamingPref;
            }
        }
        
        return false;
    }
    
    /**
     * 使用流式客户端调用LLM
     */
    private String callLLMStreaming(AgentContext context, String systemPrompt, String userPrompt) {
        try {
            // 构建调用选项
            Map<String, Object> options = new HashMap<>();
            options.put("model", getModel(context));
            options.put("maxTokens", getMaxTokens(context));
            options.put("temperature", getTemperature(context));
            
            // 获取进度监听器
            ProgressListener progressListener = getProgressListener(context);
            
            // 调用流式客户端
            Mono<String> responseMono = llmStreamingClient.streamCall(systemPrompt, userPrompt, options, progressListener);
            
            // 阻塞获取结果（在实际应用中可能需要异步处理）
            return responseMono.block(Duration.ofMillis(timeoutMillis));
            
        } catch (Exception e) {
            logger.error("流式客户端调用异常", e);
            return null;
        }
    }
    
    /**
     * 获取进度监听器
     */
    private ProgressListener getProgressListener(AgentContext context) {
        // 从上下文获取进度监听器
        Object listenerObj = context.getExtensionProperty("progressListener");
        if (listenerObj instanceof ProgressListener) {
            return (ProgressListener) listenerObj;
        }
        
        // 返回默认的空实现
        return new ProgressListener() {
            @Override
            public void onStepStarted(String stepName) {
                logger.debug("步骤开始: {}", stepName);
            }
            
            @Override
            public void onStepCompleted(String stepName) {
                logger.debug("步骤完成: {}", stepName);
            }
            
            @Override
            public void onStepFailed(String stepName, String error) {
                logger.warn("步骤失败: {} - {}", stepName, error);
            }
            
            @Override
            public void onProgress(String stepName, double percentage, String message) {
                logger.debug("进度更新: {} - {}% - {}", stepName, percentage, message);
            }
            
            @Override
            public void onData(String stepName, Object data) {
                logger.debug("数据接收: {} - {}", stepName, data);
            }
            
            @Override
            public void onCompleted(String message) {
                logger.info("处理完成: {}", message);
            }
            
            @Override
            public void onError(String error) {
                logger.error("处理错误: {}", error);
            }
        };
    }
    
    /**
     * 使用Spring AI ChatClient调用LLM
     */
    private String callLLM(AgentContext context, String systemPrompt, String userPrompt) {
        try {
            String model = getModel(context);
            int maxTokens = getMaxTokens(context);
            double temperature = getTemperature(context);
            // 组装prompt
            StringBuilder promptBuilder = new StringBuilder();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                promptBuilder.append(systemPrompt).append("\n");
            }
            if (userPrompt != null && !userPrompt.isEmpty()) {
                promptBuilder.append(userPrompt);
            }
            String prompt = promptBuilder.toString();
            // 调用ChatClient
            return chatClient.prompt()
                    .options(OpenAiChatOptions.builder()
                        .model(model)
                        .maxTokens(maxTokens)
                        .temperature(temperature)
                        .build())
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            logger.error("Spring AI ChatClient调用异常", e);
            return null;
        }
    }
    
    /**
     * 获取模型名称
     */
    private String getModel(AgentContext context) {
        Object modelObj = getConfigParameter(context, "model", defaultModel);
        return modelObj instanceof String ? (String) modelObj : defaultModel;
    }
    
    /**
     * 获取最大Token数
     */
    private int getMaxTokens(AgentContext context) {
        Object maxTokensObj = getConfigParameter(context, "maxTokens", maxTokens);
        if (maxTokensObj instanceof Integer) {
            return (Integer) maxTokensObj;
        } else if (maxTokensObj instanceof Number) {
            return ((Number) maxTokensObj).intValue();
        }
        return maxTokens;
    }
    
    /**
     * 获取温度参数
     */
    private double getTemperature(AgentContext context) {
        Object temperatureObj = getConfigParameter(context, "temperature", temperature);
        if (temperatureObj instanceof Double) {
            return (Double) temperatureObj;
        } else if (temperatureObj instanceof Number) {
            return ((Number) temperatureObj).doubleValue();
        }
        return temperature;
    }
    
    /**
     * 解析响应
     */
    private String parseResponse(String rawResponse) {
        try {
            // 简单的JSON解析（实际应用中应使用JSON库）
            // 这里假设响应格式为：{"choices":[{"message":{"content":"响应内容"}}]}
            
            if (rawResponse == null || rawResponse.trim().isEmpty()) {
                return null;
            }
            
            // 查找content字段
            int contentIndex = rawResponse.indexOf("\"content\":");
            if (contentIndex == -1) {
                return rawResponse; // 如果无法解析，返回原始响应
            }
            
            // 提取content值
            int startIndex = rawResponse.indexOf("\"", contentIndex + 10);
            if (startIndex == -1) {
                return rawResponse;
            }
            
            int endIndex = findClosingQuote(rawResponse, startIndex + 1);
            if (endIndex == -1) {
                return rawResponse;
            }
            
            String content = rawResponse.substring(startIndex + 1, endIndex);
            
            // 处理转义字符
            content = content.replace("\\n", "\n")
                           .replace("\\t", "\t")
                           .replace("\\\"", "\"")
                           .replace("\\\\", "\\");
            
            return content;
            
        } catch (Exception e) {
            logger.error("响应解析失败", e);
            return rawResponse; // 解析失败时返回原始响应
        }
    }
    
    /**
     * 查找匹配的结束引号
     */
    private int findClosingQuote(String text, int startIndex) {
        for (int i = startIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 后处理响应
     */
    private String postProcessResponse(AgentContext context, String response) {
        if (response == null || response.trim().isEmpty()) {
            return "抱歉，我无法为您提供有效的回答。";
        }
        
        // 去除多余的空白
        String processed = response.trim();
        
        // 检查响应质量
        if (processed.length() < 10) {
            processed = "抱歉，生成的回答太短，请重新提问。";
        }
        
        // 添加免责声明（如果需要）
        if (context.isDebugMode()) {
            processed += "\n\n[注：此回答由AI生成，仅供参考]";
        }
        
        return processed;
    }
    
    @Override
    public String getStepName() {
        return STEP_NAME;
    }
    
    @Override
    public boolean checkPreconditions(AgentContext context) {
        // 检查是否有构建好的提示
        String systemPrompt = getSystemPrompt(context);
        String userPrompt = getUserPrompt(context);
        return systemPrompt != null && userPrompt != null;
    }
    
    @Override
    public boolean canSkip(AgentContext context) {
        // 检查是否已经有模型响应
        Object llmResponse = context.getExtensionProperty("llmResponse");
        return llmResponse != null;
    }
    
    @Override
    public void afterExecution(AgentContext context, boolean success) {
        if (success) {
            context.setProcessingStatus("MODEL_INVOKED");
        } else {
            context.setProcessingStatus("MODEL_INVOCATION_FAILED");
        }
    }
} 