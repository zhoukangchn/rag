package com.example.agent.pipeline.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * LLM客户端配置类
 * 配置WebClient、超时、重试机制等
 */
@Configuration
public class LlmClientConfig {
    
    @Value("${agent.llm.connection.timeout:10000}")
    private int connectionTimeout;
    
    @Value("${agent.llm.read.timeout:60000}")
    private int readTimeout;
    
    @Value("${agent.llm.max-memory-size:10485760}")
    private int maxMemorySize;
    
    @Value("${agent.llm.retry.max-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${agent.llm.retry.delay:1000}")
    private long retryDelay;
    
    /**
     * 配置标准的WebClient Builder
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxMemorySize));
    }
    
    /**
     * 获取连接超时配置
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    /**
     * 获取读取超时配置
     */
    public int getReadTimeout() {
        return readTimeout;
    }
    
    /**
     * 获取最大内存大小配置
     */
    public int getMaxMemorySize() {
        return maxMemorySize;
    }
    
    /**
     * 获取最大重试次数配置
     */
    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }
    
    /**
     * 获取重试延迟配置
     */
    public long getRetryDelay() {
        return retryDelay;
    }
} 