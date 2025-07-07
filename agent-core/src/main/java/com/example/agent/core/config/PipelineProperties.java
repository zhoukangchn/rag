package com.example.agent.core.config;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;

/**
 * 处理管道配置属性
 * 
 * @author agent
 */
@Validated
public class PipelineProperties {

    /**
     * 是否启用异步处理
     */
    private boolean async = true;

    /**
     * 处理超时时间
     */
    @NotNull
    private Duration timeout = Duration.ofSeconds(60);

    /**
     * 最大并发处理数
     */
    @Positive
    private int maxConcurrency = 10;

    /**
     * 线程池核心大小
     */
    @Positive
    private int corePoolSize = 5;

    /**
     * 线程池最大大小
     */
    @Positive
    private int maxPoolSize = 20;

    /**
     * 队列容量
     */
    @Positive
    private int queueCapacity = 100;

    /**
     * 线程保活时间
     */
    @NotNull
    private Duration keepAliveTime = Duration.ofSeconds(60);

    /**
     * 是否启用重试机制
     */
    private boolean retryEnabled = true;

    /**
     * 最大重试次数
     */
    @Positive
    private int maxRetryAttempts = 3;

    /**
     * 重试延迟时间
     */
    @NotNull
    private Duration retryDelay = Duration.ofSeconds(1);

    /**
     * 是否启用性能监控
     */
    private boolean metricsEnabled = true;

    /**
     * 是否启用详细日志
     */
    private boolean verboseLogging = false;

    // Getters and Setters
    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public Duration getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(Duration keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public boolean isRetryEnabled() {
        return retryEnabled;
    }

    public void setRetryEnabled(boolean retryEnabled) {
        this.retryEnabled = retryEnabled;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(Duration retryDelay) {
        this.retryDelay = retryDelay;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }

    public boolean isVerboseLogging() {
        return verboseLogging;
    }

    public void setVerboseLogging(boolean verboseLogging) {
        this.verboseLogging = verboseLogging;
    }

    @Override
    public String toString() {
        return "PipelineProperties{" +
                "async=" + async +
                ", timeout=" + timeout +
                ", maxConcurrency=" + maxConcurrency +
                ", corePoolSize=" + corePoolSize +
                ", maxPoolSize=" + maxPoolSize +
                ", queueCapacity=" + queueCapacity +
                ", keepAliveTime=" + keepAliveTime +
                ", retryEnabled=" + retryEnabled +
                ", maxRetryAttempts=" + maxRetryAttempts +
                ", retryDelay=" + retryDelay +
                ", metricsEnabled=" + metricsEnabled +
                ", verboseLogging=" + verboseLogging +
                '}';
    }
} 