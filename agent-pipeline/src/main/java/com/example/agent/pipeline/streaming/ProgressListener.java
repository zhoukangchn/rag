package com.example.agent.pipeline.streaming;

/**
 * 进度监听器接口
 * 用于在处理过程中推送进度事件
 */
public interface ProgressListener {
    
    /**
     * 步骤开始
     */
    void onStepStarted(String stepName);
    
    /**
     * 步骤完成
     */
    void onStepCompleted(String stepName);
    
    /**
     * 步骤失败
     */
    void onStepFailed(String stepName, String error);
    
    /**
     * 处理进度更新
     */
    void onProgress(String stepName, double percentage, String message);
    
    /**
     * 数据接收
     */
    void onData(String stepName, Object data);
    
    /**
     * 完成
     */
    void onCompleted(String message);
    
    /**
     * 错误
     */
    void onError(String error);
} 