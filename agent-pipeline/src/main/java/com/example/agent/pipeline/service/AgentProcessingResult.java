package com.example.agent.pipeline.service;

import java.util.List;
import java.util.Map;

/**
 * Agent处理结果封装类
 * 包含处理过程的完整信息和结果
 * 
 * @author agent
 */
public class AgentProcessingResult {
    
    /**
     * 查询ID
     */
    private String queryId;
    
    /**
     * 是否成功
     */
    private boolean successful;
    
    /**
     * 处理响应
     */
    private String response;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 处理耗时（毫秒）
     */
    private long duration;
    
    /**
     * 知识块数量
     */
    private int knowledgeChunksCount;
    
    /**
     * 执行的步骤列表
     */
    private List<String> stepsExecuted;
    
    /**
     * 步骤执行时间
     */
    private Map<String, Long> stepExecutionTimes;
    
    /**
     * 调试信息
     */
    private Map<String, Object> debugInfo;
    
    // 构造函数
    public AgentProcessingResult() {}
    
    public AgentProcessingResult(String queryId, boolean successful, String response, long duration) {
        this.queryId = queryId;
        this.successful = successful;
        this.response = response;
        this.duration = duration;
    }
    
    // 静态工厂方法
    public static AgentProcessingResult success(String queryId, String response, long duration) {
        return new AgentProcessingResult(queryId, true, response, duration);
    }
    
    public static AgentProcessingResult error(String queryId, String errorMessage, long duration) {
        AgentProcessingResult result = new AgentProcessingResult(queryId, false, null, duration);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    // Getters and Setters
    public String getQueryId() {
        return queryId;
    }
    
    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }
    
    public boolean isSuccessful() {
        return successful;
    }
    
    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
    
    public String getResponse() {
        return response;
    }
    
    public void setResponse(String response) {
        this.response = response;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public long getDuration() {
        return duration;
    }
    
    public void setDuration(long duration) {
        this.duration = duration;
    }
    
    public int getKnowledgeChunksCount() {
        return knowledgeChunksCount;
    }
    
    public void setKnowledgeChunksCount(int knowledgeChunksCount) {
        this.knowledgeChunksCount = knowledgeChunksCount;
    }
    
    public List<String> getStepsExecuted() {
        return stepsExecuted;
    }
    
    public void setStepsExecuted(List<String> stepsExecuted) {
        this.stepsExecuted = stepsExecuted;
    }
    
    public Map<String, Long> getStepExecutionTimes() {
        return stepExecutionTimes;
    }
    
    public void setStepExecutionTimes(Map<String, Long> stepExecutionTimes) {
        this.stepExecutionTimes = stepExecutionTimes;
    }
    
    public Map<String, Object> getDebugInfo() {
        return debugInfo;
    }
    
    public void setDebugInfo(Map<String, Object> debugInfo) {
        this.debugInfo = debugInfo;
    }
    
    @Override
    public String toString() {
        return "AgentProcessingResult{" +
                "queryId='" + queryId + '\'' +
                ", successful=" + successful +
                ", response='" + (response != null && response.length() > 100 ? 
                    response.substring(0, 100) + "..." : response) + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", duration=" + duration +
                ", knowledgeChunksCount=" + knowledgeChunksCount +
                ", stepsExecuted=" + (stepsExecuted != null ? stepsExecuted.size() : 0) +
                '}';
    }
} 