package com.example.agent.pipeline.streaming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于流式服务的进度监听器
 * 将进度事件推送到SSE连接
 */
public class StreamingProgressListener implements ProgressListener {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamingProgressListener.class);
    
    private final StreamingService streamingService;
    private final String sessionId;
    
    public StreamingProgressListener(StreamingService streamingService, String sessionId) {
        this.streamingService = streamingService;
        this.sessionId = sessionId;
    }
    
    @Override
    public void onStepStarted(String stepName) {
        logger.debug("Step started: {}", stepName);
        streamingService.sendEvent(sessionId, StreamingProgressEvent.stepStarted(stepName));
    }
    
    @Override
    public void onStepCompleted(String stepName) {
        logger.debug("Step completed: {}", stepName);
        streamingService.sendEvent(sessionId, StreamingProgressEvent.stepCompleted(stepName));
    }
    
    @Override
    public void onStepFailed(String stepName, String error) {
        logger.warn("Step failed: {} - {}", stepName, error);
        streamingService.sendEvent(sessionId, StreamingProgressEvent.stepFailed(stepName, error));
    }
    
    @Override
    public void onProgress(String stepName, double percentage, String message) {
        logger.debug("Progress update: {} - {}% - {}", stepName, percentage, message);
        streamingService.sendEvent(sessionId, StreamingProgressEvent.progress(stepName, percentage, message));
    }
    
    @Override
    public void onData(String stepName, Object data) {
        logger.debug("Data received: {}", stepName);
        streamingService.sendEvent(sessionId, StreamingProgressEvent.data(stepName, data));
    }
    
    @Override
    public void onCompleted(String message) {
        logger.info("Processing completed: {}", message);
        streamingService.sendEvent(sessionId, StreamingProgressEvent.completed(message));
    }
    
    @Override
    public void onError(String error) {
        logger.error("Processing error: {}", error);
        streamingService.sendEvent(sessionId, StreamingProgressEvent.error(error));
    }
} 