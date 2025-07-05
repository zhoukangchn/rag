package com.example.agent.pipeline.streaming;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 流式处理服务
 * 负责管理SSE连接和事件推送
 */
@Service
public class StreamingService {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamingService.class);
    private static final long SSE_TIMEOUT = 30 * 60 * 1000; // 30分钟超时
    
    private final ConcurrentMap<String, SseEmitter> connections = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService heartbeatScheduler;
    
    public StreamingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.heartbeatScheduler = Executors.newScheduledThreadPool(1);
        startHeartbeat();
    }
    
    /**
     * 创建新的SSE连接
     */
    public SseEmitter createConnection(String sessionId) {
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
        }
        
        final String finalSessionId = sessionId;
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        
        // 设置连接完成和超时处理
        emitter.onCompletion(() -> {
            logger.info("SSE connection completed for session: {}", finalSessionId);
            connections.remove(finalSessionId);
        });
        
        emitter.onTimeout(() -> {
            logger.warn("SSE connection timed out for session: {}", finalSessionId);
            connections.remove(finalSessionId);
        });
        
        emitter.onError((throwable) -> {
            logger.error("SSE connection error for session: {}", finalSessionId, throwable);
            connections.remove(finalSessionId);
        });
        
        connections.put(finalSessionId, emitter);
        logger.info("Created new SSE connection for session: {}", finalSessionId);
        
        // 发送连接成功事件
        try {
            sendEvent(finalSessionId, StreamingProgressEvent.stepStarted("connection"));
        } catch (Exception e) {
            logger.error("Failed to send connection event", e);
        }
        
        return emitter;
    }
    
    /**
     * 发送事件到指定会话
     */
    public void sendEvent(String sessionId, StreamingProgressEvent event) {
        SseEmitter emitter = connections.get(sessionId);
        if (emitter == null) {
            logger.warn("No active connection found for session: {}", sessionId);
            return;
        }
        
        try {
            event.setEventId(UUID.randomUUID().toString());
            String jsonData = objectMapper.writeValueAsString(event);
            
            emitter.send(SseEmitter.event()
                    .id(event.getEventId())
                    .name(event.getEventType())
                    .data(jsonData));
            
            logger.debug("Sent event to session {}: {}", sessionId, event.getEventType());
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event", e);
        } catch (IOException e) {
            logger.error("Failed to send event to session: {}", sessionId, e);
            connections.remove(sessionId);
        } catch (Exception e) {
            logger.error("Unexpected error sending event", e);
        }
    }
    
    /**
     * 广播事件到所有连接
     */
    public void broadcastEvent(StreamingProgressEvent event) {
        if (connections.isEmpty()) {
            logger.debug("No active connections for broadcasting");
            return;
        }
        
        connections.forEach((sessionId, emitter) -> {
            try {
                sendEvent(sessionId, event);
            } catch (Exception e) {
                logger.error("Failed to broadcast event to session: {}", sessionId, e);
            }
        });
    }
    
    /**
     * 关闭指定会话的连接
     */
    public void closeConnection(String sessionId) {
        SseEmitter emitter = connections.remove(sessionId);
        if (emitter != null) {
            try {
                emitter.complete();
                logger.info("Closed SSE connection for session: {}", sessionId);
            } catch (Exception e) {
                logger.error("Error closing connection for session: {}", sessionId, e);
            }
        }
    }
    
    /**
     * 获取活跃连接数
     */
    public int getActiveConnectionCount() {
        return connections.size();
    }
    
    /**
     * 检查会话是否存在
     */
    public boolean hasConnection(String sessionId) {
        return connections.containsKey(sessionId);
    }
    
    /**
     * 发送心跳保持连接
     */
    private void startHeartbeat() {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (!connections.isEmpty()) {
                StreamingProgressEvent heartbeat = new StreamingProgressEvent("HEARTBEAT", null, "ping");
                connections.forEach((sessionId, emitter) -> {
                    try {
                        sendEvent(sessionId, heartbeat);
                    } catch (Exception e) {
                        logger.debug("Heartbeat failed for session: {}", sessionId);
                    }
                });
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    /**
     * 关闭服务时清理资源
     */
    public void shutdown() {
        logger.info("Shutting down streaming service");
        connections.values().forEach(emitter -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                logger.error("Error closing emitter during shutdown", e);
            }
        });
        connections.clear();
        heartbeatScheduler.shutdown();
    }
} 