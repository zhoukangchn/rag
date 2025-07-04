package com.example.agent.pipeline.template;

import com.example.agent.core.dto.AgentContext;
import com.example.agent.core.step.PipelineStep;
import com.example.agent.core.exception.AgentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 抽象流程步骤模板
 * 实现了责任链模式的基本功能和模板方法
 * 
 * @author agent
 */
public abstract class AbstractPipelineStep implements PipelineStep {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    /**
     * 下一个步骤
     */
    protected PipelineStep nextStep;
    
    /**
     * 执行当前步骤（模板方法）
     */
    @Override
    public final boolean execute(AgentContext context) {
        String stepName = getStepName();
        logger.debug("开始执行步骤: {}, 查询ID: {}", stepName, context.getQueryId());
        
        try {
            // 设置当前步骤
            context.setCurrentStep(stepName);
            
            // 检查前置条件
            if (!checkPreconditions(context)) {
                logger.warn("步骤 {} 前置条件不满足，跳过执行", stepName);
                return executeNext(context);
            }
            
            // 检查是否可以跳过
            if (canSkip(context)) {
                logger.info("步骤 {} 可以跳过，继续下一步", stepName);
                return executeNext(context);
            }
            
            // 执行具体步骤逻辑
            boolean success = doExecute(context);
            
            // 后置处理
            afterExecution(context, success);
            
            if (success) {
                logger.debug("步骤 {} 执行成功", stepName);
                return executeNext(context);
            } else {
                logger.warn("步骤 {} 执行失败", stepName);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("步骤 {} 执行异常: {}", stepName, e.getMessage(), e);
            context.setErrorMessage(e.getMessage());
            context.setProcessingStatus("ERROR");
            afterExecution(context, false);
            throw new AgentException("步骤执行失败: " + stepName, e);
        }
    }
    
    /**
     * 具体的步骤执行逻辑，由子类实现
     * 
     * @param context 代理上下文
     * @return 是否执行成功
     */
    protected abstract boolean doExecute(AgentContext context);
    
    /**
     * 执行下一个步骤
     * 
     * @param context 代理上下文
     * @return 是否继续执行
     */
    protected boolean executeNext(AgentContext context) {
        if (nextStep != null) {
            return nextStep.execute(context);
        }
        return true;
    }
    
    @Override
    public void setNextStep(PipelineStep nextStep) {
        this.nextStep = nextStep;
    }
    
    @Override
    public PipelineStep getNextStep() {
        return nextStep;
    }
    
    /**
     * 记录步骤执行时间
     * 
     * @param context 代理上下文
     * @param stepName 步骤名称
     * @param duration 执行时长
     */
    protected void recordExecutionTime(AgentContext context, String stepName, long duration) {
        if (context.isDebugMode()) {
            context.addExtensionProperty(stepName + "_duration", duration);
        }
        logger.debug("步骤 {} 执行耗时: {}ms", stepName, duration);
    }
    
    /**
     * 获取配置参数
     * 
     * @param context 代理上下文
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    protected Object getConfigParameter(AgentContext context, String key, Object defaultValue) {
        if (context.getRetrievalParams() != null && context.getRetrievalParams().containsKey(key)) {
            return context.getRetrievalParams().get(key);
        }
        return defaultValue;
    }
    
    /**
     * 验证必要的上下文信息
     * 
     * @param context 代理上下文
     * @param requiredFields 必需字段
     * @return 是否验证通过
     */
    protected boolean validateContext(AgentContext context, String... requiredFields) {
        for (String field : requiredFields) {
            switch (field) {
                case "query":
                    if (context.getCurrentQuery() == null || context.getCurrentQuery().trim().isEmpty()) {
                        logger.error("验证失败: 查询内容为空");
                        return false;
                    }
                    break;
                case "sessionId":
                    if (context.getSessionId() == null || context.getSessionId().trim().isEmpty()) {
                        logger.error("验证失败: 会话ID为空");
                        return false;
                    }
                    break;
                case "userId":
                    if (context.getUserId() == null || context.getUserId().trim().isEmpty()) {
                        logger.error("验证失败: 用户ID为空");
                        return false;
                    }
                    break;
                default:
                    logger.warn("未知的验证字段: {}", field);
            }
        }
        return true;
    }
} 