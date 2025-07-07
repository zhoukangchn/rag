package com.example.agent.core.config;

import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * LLM客户端配置属性
 * 
 * @author agent
 */
@Validated
public class LlmProperties {

    /**
     * 连接配置
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private ConnectionProperties connection = new ConnectionProperties();

    /**
     * 重试配置
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private RetryProperties retry = new RetryProperties();

    /**
     * API配置
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private ApiProperties api = new ApiProperties();

    /**
     * 流式处理配置
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private StreamProperties stream = new StreamProperties();

    /**
     * 默认模型配置
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private DefaultModelProperties defaultModel = new DefaultModelProperties();

    /**
     * 读取超时时间
     */
    @NotNull
    private Duration readTimeout = Duration.ofSeconds(60);

    /**
     * 最大内存大小
     */
    @Positive
    private long maxMemorySize = 10485760L; // 10MB

    // Getters and Setters
    public ConnectionProperties getConnection() {
        return connection;
    }

    public void setConnection(ConnectionProperties connection) {
        this.connection = connection;
    }

    public RetryProperties getRetry() {
        return retry;
    }

    public void setRetry(RetryProperties retry) {
        this.retry = retry;
    }

    public ApiProperties getApi() {
        return api;
    }

    public void setApi(ApiProperties api) {
        this.api = api;
    }

    public StreamProperties getStream() {
        return stream;
    }

    public void setStream(StreamProperties stream) {
        this.stream = stream;
    }

    public DefaultModelProperties getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(DefaultModelProperties defaultModel) {
        this.defaultModel = defaultModel;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public long getMaxMemorySize() {
        return maxMemorySize;
    }

    public void setMaxMemorySize(long maxMemorySize) {
        this.maxMemorySize = maxMemorySize;
    }

    /**
     * 连接配置
     */
    @Validated
    public static class ConnectionProperties {
        /**
         * 连接超时时间
         */
        @NotNull
        private Duration timeout = Duration.ofSeconds(10);

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        @Override
        public String toString() {
            return "ConnectionProperties{timeout=" + timeout + '}';
        }
    }

    /**
     * 重试配置
     */
    @Validated
    public static class RetryProperties {
        /**
         * 最大重试次数
         */
        @Min(1)
        @Max(10)
        private int maxAttempts = 3;

        /**
         * 重试延迟时间（毫秒）
         */
        @Positive
        private long delay = 1000L;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getDelay() {
            return delay;
        }

        public void setDelay(long delay) {
            this.delay = delay;
        }

        @Override
        public String toString() {
            return "RetryProperties{maxAttempts=" + maxAttempts + ", delay=" + delay + '}';
        }
    }

    /**
     * API配置
     */
    @Validated
    public static class ApiProperties {
        /**
         * OpenAI API配置
         */
        @NestedConfigurationProperty
        @Valid
        @NotNull
        private ApiEndpointProperties openai = new ApiEndpointProperties(
            "https://api.openai.com/v1/chat/completions", "");

        /**
         * Claude API配置
         */
        @NestedConfigurationProperty
        @Valid
        @NotNull
        private ApiEndpointProperties claude = new ApiEndpointProperties(
            "https://api.anthropic.com/v1/messages", "");

        /**
         * 本地API配置
         */
        @NestedConfigurationProperty
        @Valid
        @NotNull
        private ApiEndpointProperties local = new ApiEndpointProperties(
            "http://localhost:11434/api/chat", "");

        public ApiEndpointProperties getOpenai() {
            return openai;
        }

        public void setOpenai(ApiEndpointProperties openai) {
            this.openai = openai;
        }

        public ApiEndpointProperties getClaude() {
            return claude;
        }

        public void setClaude(ApiEndpointProperties claude) {
            this.claude = claude;
        }

        public ApiEndpointProperties getLocal() {
            return local;
        }

        public void setLocal(ApiEndpointProperties local) {
            this.local = local;
        }

        @Override
        public String toString() {
            return "ApiProperties{openai=" + openai + ", claude=" + claude + ", local=" + local + '}';
        }
    }

    /**
     * API端点配置
     */
    @Validated
    public static class ApiEndpointProperties {
        /**
         * API端点URL
         */
        @NotBlank
        private String endpoint;

        /**
         * API密钥
         */
        private String key;

        public ApiEndpointProperties() {}

        public ApiEndpointProperties(String endpoint, String key) {
            this.endpoint = endpoint;
            this.key = key;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return "ApiEndpointProperties{endpoint='" + endpoint + "', key='" + key + "'}";
        }
    }

    /**
     * 流式处理配置
     */
    @Validated
    public static class StreamProperties {
        /**
         * 流式处理端点
         */
        @NotBlank
        private String endpoint = "https://api.openai.com/v1/chat/completions";

        /**
         * 流式处理超时时间
         */
        @NotNull
        private Duration timeout = Duration.ofSeconds(60);

        /**
         * 缓冲区大小
         */
        @Positive
        private int bufferSize = 1024;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public int getBufferSize() {
            return bufferSize;
        }

        public void setBufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
        }

        @Override
        public String toString() {
            return "StreamProperties{endpoint='" + endpoint + "', timeout=" + timeout + 
                   ", bufferSize=" + bufferSize + '}';
        }
    }

    /**
     * 默认模型配置
     */
    @Validated
    public static class DefaultModelProperties {
        /**
         * 默认模型名称
         */
        @NotBlank
        private String model = "gpt-3.5-turbo";

        /**
         * 温度参数
         */
        @Min(0)
        @Max(2)
        private double temperature = 0.7;

        /**
         * 最大令牌数
         */
        @Positive
        private int maxTokens = 2000;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        @Override
        public String toString() {
            return "DefaultModelProperties{model='" + model + "', temperature=" + temperature + 
                   ", maxTokens=" + maxTokens + '}';
        }
    }

    @Override
    public String toString() {
        return "LlmProperties{" +
                "connection=" + connection +
                ", retry=" + retry +
                ", api=" + api +
                ", stream=" + stream +
                ", defaultModel=" + defaultModel +
                ", readTimeout=" + readTimeout +
                ", maxMemorySize=" + maxMemorySize +
                '}';
    }
} 