package com.example.agent.pipeline.step;

import com.example.agent.core.dto.AgentContext;
import com.example.agent.pipeline.template.AbstractPipelineStep;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
    
    public ModelInvocationStep() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
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
            
            // 调用LLM
            String response = callLLM(context, systemPrompt, userPrompt);
            
            if (response == null) {
                logger.error("模型调用失败，未返回响应");
                return false;
            }
            
            // 后处理响应
            String processedResponse = postProcessResponse(context, response);
            
            // 保存响应到上下文
            context.addExtensionProperty("llmResponse", processedResponse);
            context.addExtensionProperty("responseLength", processedResponse.length());
            
            // 记录执行时间
            long duration = System.currentTimeMillis() - startTime;
            recordExecutionTime(context, STEP_NAME, duration);
            
            logger.info("模型调用完成 - 查询ID: {}, 响应长度: {}, 耗时: {}ms", 
                    context.getQueryId(), processedResponse.length(), duration);
            
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
     * 调用LLM
     */
    private String callLLM(AgentContext context, String systemPrompt, String userPrompt) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = buildRequestBody(context, systemPrompt, userPrompt);
            
            // 发送请求
            Mono<String> responseMono = webClient.post()
                    .uri(llmEndpoint)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeoutMillis));
            
            // 同步等待响应
            String rawResponse = responseMono.block();
            
            // 解析响应
            return parseResponse(rawResponse);
            
        } catch (Exception e) {
            logger.error("LLM调用异常", e);
            return null;
        }
    }
    
    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(AgentContext context, String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = new HashMap<>();
        
        // 基础参数
        requestBody.put("model", getModel(context));
        requestBody.put("max_tokens", getMaxTokens(context));
        requestBody.put("temperature", getTemperature(context));
        requestBody.put("stream", false); // 暂不支持流式
        
        // 构建消息数组
        java.util.List<Map<String, String>> messages = new java.util.ArrayList<>();
        
        // 系统消息
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);
        
        // 用户消息
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);
        
        requestBody.put("messages", messages);
        
        return requestBody;
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