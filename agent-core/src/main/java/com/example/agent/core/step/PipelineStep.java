package com.example.agent.core.step;

import com.example.agent.core.dto.AgentContext;

/**
 * 责任链处理步骤接口
 * 定义了处理流程中每个步骤的通用方法
 * 
 * @author agent
 */
public interface PipelineStep {
    
    /**
     * 执行当前步骤
     * 
     * @param context 代理上下文
     * @return 是否继续执行下一步骤
     */
    boolean execute(AgentContext context);
    
    /**
     * 获取步骤名称
     * 
     * @return 步骤名称
     */
    String getStepName();
    
    /**
     * 设置下一个步骤
     * 
     * @param nextStep 下一个步骤
     */
    void setNextStep(PipelineStep nextStep);
    
    /**
     * 获取下一个步骤
     * 
     * @return 下一个步骤
     */
    PipelineStep getNextStep();
    
    /**
     * 是否可以跳过当前步骤
     * 
     * @param context 代理上下文
     * @return 是否可以跳过
     */
    default boolean canSkip(AgentContext context) {
        return false;
    }
    
    /**
     * 步骤前置条件检查
     * 
     * @param context 代理上下文
     * @return 是否满足前置条件
     */
    default boolean checkPreconditions(AgentContext context) {
        return true;
    }
    
    /**
     * 步骤后置处理
     * 
     * @param context 代理上下文
     * @param success 执行是否成功
     */
    default void afterExecution(AgentContext context, boolean success) {
        // 默认实现为空
    }
} 