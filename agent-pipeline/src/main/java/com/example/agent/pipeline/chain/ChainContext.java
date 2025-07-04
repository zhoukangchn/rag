package com.example.agent.pipeline.chain;

import com.example.agent.core.dto.AgentContext;
import com.example.agent.core.step.PipelineStep;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 责任链执行上下文
 * 管理责任链的执行状态和统计信息
 * 
 * @author agent
 */
public class ChainContext {
    
    /**
     * 代理上下文
     */
    private AgentContext agentContext;
    
    /**
     * 责任链ID
     */
    private String chainId;
    
    /**
     * 责任链名称
     */
    private String chainName;
    
    /**
     * 当前执行步骤
     */
    private PipelineStep currentStep;
    
    /**
     * 已执行步骤列表
     */
    private List<String> executedSteps;
    
    /**
     * 步骤执行时间统计
     */
    private Map<String, Long> stepExecutionTimes;
    
    /**
     * 步骤执行结果
     */
    private Map<String, Boolean> stepExecutionResults;
    
    /**
     * 执行开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 执行结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 总执行时间（毫秒）
     */
    private long totalExecutionTime;
    
    /**
     * 是否执行成功
     */
    private boolean successful;
    
    /**
     * 是否已完成
     */
    private boolean completed;
    
    /**
     * 异常信息
     */
    private Exception exception;
    
    /**
     * 重试次数
     */
    private int retryCount;
    
    /**
     * 最大重试次数
     */
    private int maxRetryCount;
    
    /**
     * 扩展属性
     */
    private Map<String, Object> extensionProperties;
    
    // 构造函数
    public ChainContext() {
        this.executedSteps = new ArrayList<>();
        this.stepExecutionTimes = new HashMap<>();
        this.stepExecutionResults = new HashMap<>();
        this.startTime = LocalDateTime.now();
        this.successful = false;
        this.completed = false;
        this.retryCount = 0;
        this.maxRetryCount = 3;
        this.extensionProperties = new HashMap<>();
    }
    
    public ChainContext(AgentContext agentContext, String chainId, String chainName) {
        this();
        this.agentContext = agentContext;
        this.chainId = chainId;
        this.chainName = chainName;
    }
    
    // 业务方法
    
    /**
     * 开始执行
     */
    public void start() {
        this.startTime = LocalDateTime.now();
        this.completed = false;
        this.successful = false;
    }
    
    /**
     * 完成执行
     */
    public void complete(boolean successful) {
        this.endTime = LocalDateTime.now();
        this.completed = true;
        this.successful = successful;
        this.totalExecutionTime = java.time.Duration.between(startTime, endTime).toMillis();
    }
    
    /**
     * 记录步骤执行
     */
    public void recordStepExecution(String stepName, long duration, boolean success) {
        this.executedSteps.add(stepName);
        this.stepExecutionTimes.put(stepName, duration);
        this.stepExecutionResults.put(stepName, success);
    }
    
    /**
     * 设置异常
     */
    public void setException(Exception exception) {
        this.exception = exception;
        this.successful = false;
    }
    
    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }
    
    /**
     * 是否可以重试
     */
    public boolean canRetry() {
        return this.retryCount < this.maxRetryCount;
    }
    
    /**
     * 重置重试状态
     */
    public void resetRetryState() {
        this.retryCount = 0;
        this.exception = null;
        this.executedSteps.clear();
        this.stepExecutionTimes.clear();
        this.stepExecutionResults.clear();
    }
    
    /**
     * 获取执行统计信息
     */
    public Map<String, Object> getExecutionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("chainId", chainId);
        stats.put("chainName", chainName);
        stats.put("totalExecutionTime", totalExecutionTime);
        stats.put("executedSteps", executedSteps.size());
        stats.put("successful", successful);
        stats.put("completed", completed);
        stats.put("retryCount", retryCount);
        stats.put("stepExecutionTimes", new HashMap<>(stepExecutionTimes));
        stats.put("stepExecutionResults", new HashMap<>(stepExecutionResults));
        return stats;
    }
    
    /**
     * 添加扩展属性
     */
    public void addExtensionProperty(String key, Object value) {
        this.extensionProperties.put(key, value);
    }
    
    /**
     * 获取扩展属性
     */
    public Object getExtensionProperty(String key) {
        return this.extensionProperties.get(key);
    }
    
    // Getters and Setters
    public AgentContext getAgentContext() {
        return agentContext;
    }
    
    public void setAgentContext(AgentContext agentContext) {
        this.agentContext = agentContext;
    }
    
    public String getChainId() {
        return chainId;
    }
    
    public void setChainId(String chainId) {
        this.chainId = chainId;
    }
    
    public String getChainName() {
        return chainName;
    }
    
    public void setChainName(String chainName) {
        this.chainName = chainName;
    }
    
    public PipelineStep getCurrentStep() {
        return currentStep;
    }
    
    public void setCurrentStep(PipelineStep currentStep) {
        this.currentStep = currentStep;
    }
    
    public List<String> getExecutedSteps() {
        return executedSteps;
    }
    
    public Map<String, Long> getStepExecutionTimes() {
        return stepExecutionTimes;
    }
    
    public Map<String, Boolean> getStepExecutionResults() {
        return stepExecutionResults;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public long getTotalExecutionTime() {
        return totalExecutionTime;
    }
    
    public boolean isSuccessful() {
        return successful;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public Exception getException() {
        return exception;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public int getMaxRetryCount() {
        return maxRetryCount;
    }
    
    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }
    
    public Map<String, Object> getExtensionProperties() {
        return extensionProperties;
    }
    
    @Override
    public String toString() {
        return "ChainContext{" +
                "chainId='" + chainId + '\'' +
                ", chainName='" + chainName + '\'' +
                ", totalExecutionTime=" + totalExecutionTime +
                ", successful=" + successful +
                ", completed=" + completed +
                ", retryCount=" + retryCount +
                ", executedSteps=" + executedSteps.size() +
                '}';
    }
} 