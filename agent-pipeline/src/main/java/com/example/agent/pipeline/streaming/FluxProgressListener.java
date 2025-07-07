package com.example.agent.pipeline.streaming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * 基于Flux的进度监听器
 * 使用响应式流处理进度事件
 */
public class FluxProgressListener implements ProgressListener {
    
    private static final Logger logger = LoggerFactory.getLogger(FluxProgressListener.class);
    
    private final Sinks.Many<StreamingProgressEvent> eventSink;
    private final String sessionId;
    
    public FluxProgressListener(String sessionId) {
        this.sessionId = sessionId;
        this.eventSink = Sinks.many().multicast().onBackpressureBuffer();
    }
    
    /**
     * 获取事件流
     */
    public Flux<StreamingProgressEvent> getEventStream() {
        return eventSink.asFlux()
                .doOnSubscribe(subscription -> {
                    logger.info("New subscriber to flux stream for session: {}", sessionId);
                })
                .doOnCancel(() -> {
                    logger.info("Subscriber cancelled flux stream for session: {}", sessionId);
                })
                .doOnComplete(() -> {
                    logger.info("Flux stream completed for session: {}", sessionId);
                })
                .doOnError(error -> {
                    logger.error("Error in flux stream for session: {}", sessionId, error);
                });
    }
    
    @Override
    public void onStepStarted(String stepName) {
        logger.debug("Step started: {}", stepName);
        emitEvent(StreamingProgressEvent.stepStarted(stepName));
    }
    
    @Override
    public void onStepCompleted(String stepName) {
        logger.debug("Step completed: {}", stepName);
        emitEvent(StreamingProgressEvent.stepCompleted(stepName));
    }
    
    @Override
    public void onStepFailed(String stepName, String error) {
        logger.warn("Step failed: {} - {}", stepName, error);
        emitEvent(StreamingProgressEvent.stepFailed(stepName, error));
    }
    
    @Override
    public void onProgress(String stepName, double percentage, String message) {
        logger.debug("Progress update: {} - {}% - {}", stepName, percentage, message);
        emitEvent(StreamingProgressEvent.progress(stepName, percentage, message));
    }
    
    @Override
    public void onData(String stepName, Object data) {
        logger.debug("Data received: {}", stepName);
        emitEvent(StreamingProgressEvent.data(stepName, data));
    }
    
    @Override
    public void onCompleted(String message) {
        logger.info("Processing completed: {}", message);
        emitEvent(StreamingProgressEvent.completed(message));
        // 完成流
        eventSink.tryEmitComplete();
    }
    
    @Override
    public void onError(String error) {
        logger.error("Processing error: {}", error);
        emitEvent(StreamingProgressEvent.error(error));
        // 发送错误信号
        eventSink.tryEmitError(new RuntimeException(error));
    }
    
    /**
     * 发送事件到Flux流
     */
    private void emitEvent(StreamingProgressEvent event) {
        Sinks.EmitResult result = eventSink.tryEmitNext(event);
        
        if (result.isFailure()) {
            logger.warn("Failed to emit event for session {}: {}", sessionId, result);
            
            // 如果发送失败，尝试重试
            switch (result) {
                case FAIL_OVERFLOW:
                    logger.warn("Event sink overflow for session: {}", sessionId);
                    break;
                case FAIL_CANCELLED:
                    logger.warn("Event sink cancelled for session: {}", sessionId);
                    break;
                case FAIL_TERMINATED:
                    logger.warn("Event sink terminated for session: {}", sessionId);
                    break;
                default:
                    logger.warn("Unknown emit failure for session: {}", sessionId);
            }
        }
    }
    
    /**
     * 关闭Flux流
     */
    public void close() {
        logger.info("Closing flux progress listener for session: {}", sessionId);
        eventSink.tryEmitComplete();
    }
    
    /**
     * 获取会话ID
     */
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * 检查流是否已终止
     */
    public boolean isTerminated() {
        return eventSink.currentSubscriberCount() == 0;
    }
} 