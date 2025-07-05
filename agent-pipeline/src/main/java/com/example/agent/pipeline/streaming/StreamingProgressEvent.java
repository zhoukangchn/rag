package com.example.agent.pipeline.streaming;

import java.time.LocalDateTime;

/**
 * 流式处理进度事件
 */
public class StreamingProgressEvent {
    
    private String eventId;
    private String eventType;
    private String stepName;
    private String message;
    private Object data;
    private Double progressPercentage;
    private LocalDateTime timestamp;
    private String status;
    private String error;
    
    public StreamingProgressEvent() {
        this.timestamp = LocalDateTime.now();
    }
    
    public StreamingProgressEvent(String eventType, String stepName, String message) {
        this();
        this.eventType = eventType;
        this.stepName = stepName;
        this.message = message;
    }
    
    // Static factory methods for common event types
    public static StreamingProgressEvent stepStarted(String stepName) {
        return new StreamingProgressEvent("STEP_STARTED", stepName, "Step started: " + stepName);
    }
    
    public static StreamingProgressEvent stepCompleted(String stepName) {
        return new StreamingProgressEvent("STEP_COMPLETED", stepName, "Step completed: " + stepName);
    }
    
    public static StreamingProgressEvent stepFailed(String stepName, String error) {
        StreamingProgressEvent event = new StreamingProgressEvent("STEP_FAILED", stepName, "Step failed: " + stepName);
        event.setError(error);
        return event;
    }
    
    public static StreamingProgressEvent progress(String stepName, double percentage, String message) {
        StreamingProgressEvent event = new StreamingProgressEvent("PROGRESS", stepName, message);
        event.setProgressPercentage(percentage);
        return event;
    }
    
    public static StreamingProgressEvent data(String stepName, Object data) {
        StreamingProgressEvent event = new StreamingProgressEvent("DATA", stepName, "Data received");
        event.setData(data);
        return event;
    }
    
    public static StreamingProgressEvent completed(String message) {
        return new StreamingProgressEvent("COMPLETED", null, message);
    }
    
    public static StreamingProgressEvent error(String error) {
        StreamingProgressEvent event = new StreamingProgressEvent("ERROR", null, "Processing error");
        event.setError(error);
        return event;
    }
    
    // Getters and setters
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getStepName() {
        return stepName;
    }
    
    public void setStepName(String stepName) {
        this.stepName = stepName;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    public Double getProgressPercentage() {
        return progressPercentage;
    }
    
    public void setProgressPercentage(Double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    @Override
    public String toString() {
        return "StreamingProgressEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", stepName='" + stepName + '\'' +
                ", message='" + message + '\'' +
                ", progressPercentage=" + progressPercentage +
                ", timestamp=" + timestamp +
                ", status='" + status + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
} 