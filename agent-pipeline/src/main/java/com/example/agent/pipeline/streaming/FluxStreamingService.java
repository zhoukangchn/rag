package com.example.agent.pipeline.streaming;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于Flux的流式服务
 * 管理响应式流连接和事件分发
 */
@Service
public class FluxStreamingService {
    
    private static final Logger logger = LoggerFactory.getLogger(FluxStreamingService.class);
    
    private final Map<String, FluxProgressListener> activeListeners = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    
    public FluxStreamingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * 创建新的Flux监听器
     */
    public FluxProgressListener createListener(String sessionId) {
        if (sessionId == null) {
            sessionId = "flux_" + UUID.randomUUID().toString();
        }
        
        FluxProgressListener listener = new FluxProgressListener(sessionId);
        activeListeners.put(sessionId, listener);
        
        logger.info("Created new flux listener for session: {}", sessionId);
        return listener;
    }
    
    /**
     * 获取JSON格式的事件流
     */
    public Flux<String> getJsonEventStream(String sessionId) {
        FluxProgressListener listener = activeListeners.get(sessionId);
        if (listener == null) {
            logger.warn("No listener found for session: {}", sessionId);
            return Flux.error(new IllegalArgumentException("Session not found: " + sessionId));
        }
        
        return listener.getEventStream()
                .map(event -> {
                    try {
                        return objectMapper.writeValueAsString(event);
                    } catch (JsonProcessingException e) {
                        logger.error("Failed to serialize event", e);
                        return "{\"error\":\"Serialization failed\"}";
                    }
                })
                .doFinally(signalType -> {
                    logger.info("JSON event stream finished for session {} with signal: {}", sessionId, signalType);
                    cleanup(sessionId);
                });
    }
    
    /**
     * 获取SSE格式的事件流
     */
    public Flux<ServerSentEvent<String>> getSseEventStream(String sessionId) {
        FluxProgressListener listener = activeListeners.get(sessionId);
        if (listener == null) {
            logger.warn("No listener found for session: {}", sessionId);
            return Flux.error(new IllegalArgumentException("Session not found: " + sessionId));
        }
        
        return listener.getEventStream()
                .map(event -> {
                    try {
                        String jsonData = objectMapper.writeValueAsString(event);
                        return ServerSentEvent.<String>builder()
                                .id(event.getEventId() != null ? event.getEventId() : UUID.randomUUID().toString())
                                .event(event.getEventType())
                                .data(jsonData)
                                .build();
                    } catch (JsonProcessingException e) {
                        logger.error("Failed to serialize SSE event", e);
                        return ServerSentEvent.<String>builder()
                                .event("error")
                                .data("{\"error\":\"Serialization failed\"}")
                                .build();
                    }
                })
                .doFinally(signalType -> {
                    logger.info("SSE event stream finished for session {} with signal: {}", sessionId, signalType);
                    cleanup(sessionId);
                });
    }
    
    /**
     * 获取带心跳的SSE事件流
     */
    public Flux<ServerSentEvent<String>> getSseEventStreamWithHeartbeat(String sessionId) {
        Flux<ServerSentEvent<String>> eventStream = getSseEventStream(sessionId);
        
        // 创建心跳流
        Flux<ServerSentEvent<String>> heartbeatStream = Flux.interval(Duration.ofSeconds(30))
                .map(tick -> ServerSentEvent.<String>builder()
                        .event("heartbeat")
                        .data("{\"type\":\"ping\",\"timestamp\":" + System.currentTimeMillis() + "}")
                        .build())
                .takeUntil(event -> !hasActiveListener(sessionId));
        
        return Flux.merge(eventStream, heartbeatStream)
                .subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * 获取监听器
     */
    public FluxProgressListener getListener(String sessionId) {
        return activeListeners.get(sessionId);
    }
    
    /**
     * 检查是否有活跃的监听器
     */
    public boolean hasActiveListener(String sessionId) {
        FluxProgressListener listener = activeListeners.get(sessionId);
        return listener != null && !listener.isTerminated();
    }
    
    /**
     * 关闭指定会话的监听器
     */
    public Mono<Void> closeListener(String sessionId) {
        return Mono.fromRunnable(() -> {
            FluxProgressListener listener = activeListeners.remove(sessionId);
            if (listener != null) {
                listener.close();
                logger.info("Closed flux listener for session: {}", sessionId);
            } else {
                logger.warn("No listener found to close for session: {}", sessionId);
            }
        });
    }
    
    /**
     * 获取活跃连接数
     */
    public int getActiveListenerCount() {
        return activeListeners.size();
    }
    
    /**
     * 获取所有活跃会话ID
     */
    public java.util.Set<String> getActiveSessionIds() {
        return activeListeners.keySet();
    }
    
    /**
     * 广播事件到所有活跃监听器
     */
    public Mono<Void> broadcastEvent(StreamingProgressEvent event) {
        return Flux.fromIterable(activeListeners.values())
                .flatMap(listener -> Mono.fromRunnable(() -> {
                    try {
                        // 根据事件类型调用相应的监听器方法
                        switch (event.getEventType()) {
                            case "STEP_STARTED":
                                listener.onStepStarted(event.getStepName());
                                break;
                            case "STEP_COMPLETED":
                                listener.onStepCompleted(event.getStepName());
                                break;
                            case "STEP_FAILED":
                                listener.onStepFailed(event.getStepName(), event.getError());
                                break;
                            case "PROGRESS":
                                listener.onProgress(event.getStepName(), 
                                        event.getProgressPercentage() != null ? event.getProgressPercentage() : 0.0, 
                                        event.getMessage());
                                break;
                            case "DATA":
                                listener.onData(event.getStepName(), event.getData());
                                break;
                            case "COMPLETED":
                                listener.onCompleted(event.getMessage());
                                break;
                            case "ERROR":
                                listener.onError(event.getError());
                                break;
                            default:
                                logger.warn("Unknown event type: {}", event.getEventType());
                        }
                    } catch (Exception e) {
                        logger.error("Failed to broadcast event to listener: {}", listener.getSessionId(), e);
                    }
                }))
                .then();
    }
    
    /**
     * 清理资源
     */
    private void cleanup(String sessionId) {
        FluxProgressListener listener = activeListeners.remove(sessionId);
        if (listener != null) {
            logger.info("Cleaned up resources for session: {}", sessionId);
        }
    }
    
    /**
     * 关闭所有监听器
     */
    public Mono<Void> shutdownAll() {
        return Flux.fromIterable(activeListeners.keySet())
                .flatMap(this::closeListener)
                .then()
                .doOnSuccess(v -> logger.info("All flux listeners have been shutdown"));
    }
    
    /**
     * 获取服务状态信息
     */
    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("activeListeners", getActiveListenerCount());
        status.put("activeSessions", getActiveSessionIds());
        status.put("timestamp", System.currentTimeMillis());
        status.put("serviceType", "FluxStreamingService");
        return status;
    }
} 