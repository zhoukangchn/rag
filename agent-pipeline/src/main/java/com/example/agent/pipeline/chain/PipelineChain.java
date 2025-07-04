package com.example.agent.pipeline.chain;

import com.example.agent.core.dto.AgentContext;
import com.example.agent.core.step.PipelineStep;
import com.example.agent.core.exception.AgentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 责任链处理框架
 * 负责组装和执行责任链，支持重试和错误处理
 * 
 * @author agent
 */
public class PipelineChain {
    
    private static final Logger logger = LoggerFactory.getLogger(PipelineChain.class);
    
    /**
     * 责任链ID
     */
    private String chainId;
    
    /**
     * 责任链名称
     */
    private String chainName;
    
    /**
     * 步骤列表
     */
    private List<PipelineStep> steps;
    
    /**
     * 第一个步骤
     */
    private PipelineStep firstStep;
    
    /**
     * 是否已构建
     */
    private boolean built;
    
    /**
     * 构造函数
     */
    public PipelineChain() {
        this.steps = new ArrayList<>();
        this.built = false;
    }
    
    public PipelineChain(String chainId, String chainName) {
        this();
        this.chainId = chainId;
        this.chainName = chainName;
    }
    
    /**
     * 添加步骤
     */
    public PipelineChain addStep(PipelineStep step) {
        if (step == null) {
            throw new IllegalArgumentException("步骤不能为null");
        }
        this.steps.add(step);
        this.built = false; // 需要重新构建
        return this;
    }
    
    /**
     * 批量添加步骤
     */
    public PipelineChain addSteps(List<PipelineStep> steps) {
        if (steps != null) {
            for (PipelineStep step : steps) {
                addStep(step);
            }
        }
        return this;
    }
    
    /**
     * 构建责任链
     */
    public PipelineChain build() {
        if (steps.isEmpty()) {
            throw new IllegalStateException("责任链中没有步骤");
        }
        
        // 链接步骤
        for (int i = 0; i < steps.size() - 1; i++) {
            PipelineStep currentStep = steps.get(i);
            PipelineStep nextStep = steps.get(i + 1);
            currentStep.setNextStep(nextStep);
        }
        
        // 最后一个步骤的下一步为null
        if (!steps.isEmpty()) {
            steps.get(steps.size() - 1).setNextStep(null);
            this.firstStep = steps.get(0);
        }
        
        this.built = true;
        logger.info("责任链构建完成 - ID: {}, 名称: {}, 步骤数: {}", 
                chainId, chainName, steps.size());
        
        return this;
    }
    
    /**
     * 执行责任链
     */
    public ChainContext execute(AgentContext agentContext) {
        if (!built) {
            throw new IllegalStateException("责任链未构建，请先调用build()方法");
        }
        
        // 创建链上下文
        ChainContext chainContext = new ChainContext(agentContext, chainId, chainName);
        
        // 执行责任链
        return executeWithRetry(chainContext);
    }
    
    /**
     * 带重试的执行
     */
    private ChainContext executeWithRetry(ChainContext chainContext) {
        while (true) {
            try {
                // 开始执行
                chainContext.start();
                
                logger.info("开始执行责任链 - ID: {}, 名称: {}, 重试次数: {}", 
                        chainId, chainName, chainContext.getRetryCount());
                
                // 执行步骤
                boolean success = executeSteps(chainContext);
                
                // 完成执行
                chainContext.complete(success);
                
                if (success) {
                    logger.info("责任链执行成功 - ID: {}, 耗时: {}ms", 
                            chainId, chainContext.getTotalExecutionTime());
                } else {
                    logger.warn("责任链执行失败 - ID: {}, 重试次数: {}", 
                            chainId, chainContext.getRetryCount());
                }
                
                // 如果成功或无法重试，退出循环
                if (success || !chainContext.canRetry()) {
                    break;
                }
                
                // 准备重试
                chainContext.incrementRetryCount();
                chainContext.resetRetryState();
                
                logger.info("准备重试责任链 - ID: {}, 重试次数: {}", 
                        chainId, chainContext.getRetryCount());
                
            } catch (Exception e) {
                logger.error("责任链执行异常 - ID: {}", chainId, e);
                chainContext.setException(e);
                chainContext.complete(false);
                
                // 如果无法重试，退出循环
                if (!chainContext.canRetry()) {
                    break;
                }
                
                // 准备重试
                chainContext.incrementRetryCount();
                chainContext.resetRetryState();
            }
        }
        
        return chainContext;
    }
    
    /**
     * 执行步骤
     */
    private boolean executeSteps(ChainContext chainContext) {
        if (firstStep == null) {
            logger.error("责任链中没有步骤");
            return false;
        }
        
        PipelineStep currentStep = firstStep;
        
        while (currentStep != null) {
            long stepStartTime = System.currentTimeMillis();
            
            try {
                // 设置当前步骤
                chainContext.setCurrentStep(currentStep);
                
                String stepName = currentStep.getStepName();
                logger.debug("执行步骤: {}", stepName);
                
                // 执行步骤
                boolean success = currentStep.execute(chainContext.getAgentContext());
                
                // 记录执行统计
                long stepDuration = System.currentTimeMillis() - stepStartTime;
                chainContext.recordStepExecution(stepName, stepDuration, success);
                
                if (!success) {
                    logger.error("步骤执行失败: {}", stepName);
                    return false;
                }
                
                logger.debug("步骤执行成功: {}, 耗时: {}ms", stepName, stepDuration);
                
                // 移动到下一个步骤
                currentStep = currentStep.getNextStep();
                
            } catch (Exception e) {
                logger.error("步骤执行异常: {}", currentStep.getStepName(), e);
                
                // 记录异常统计
                long stepDuration = System.currentTimeMillis() - stepStartTime;
                chainContext.recordStepExecution(currentStep.getStepName(), stepDuration, false);
                
                // 将异常包装并抛出
                throw new AgentException("步骤执行失败: " + currentStep.getStepName(), e);
            }
        }
        
        return true;
    }
    
    /**
     * 获取步骤列表
     */
    public List<PipelineStep> getSteps() {
        return new ArrayList<>(steps);
    }
    
    /**
     * 获取步骤名称列表
     */
    public List<String> getStepNames() {
        return steps.stream()
                .map(PipelineStep::getStepName)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * 清空步骤
     */
    public void clear() {
        this.steps.clear();
        this.firstStep = null;
        this.built = false;
    }
    
    /**
     * 获取步骤数量
     */
    public int getStepCount() {
        return steps.size();
    }
    
    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return steps.isEmpty();
    }
    
    /**
     * 是否已构建
     */
    public boolean isBuilt() {
        return built;
    }
    
    /**
     * 验证责任链配置
     */
    public boolean validate() {
        if (steps.isEmpty()) {
            logger.error("责任链验证失败: 没有步骤");
            return false;
        }
        
        // 检查步骤名称是否重复
        Set<String> stepNames = new HashSet<>();
        for (PipelineStep step : steps) {
            String stepName = step.getStepName();
            if (stepName == null || stepName.trim().isEmpty()) {
                logger.error("责任链验证失败: 步骤名称为空");
                return false;
            }
            if (stepNames.contains(stepName)) {
                logger.error("责任链验证失败: 步骤名称重复 - {}", stepName);
                return false;
            }
            stepNames.add(stepName);
        }
        
        return true;
    }
    
    // Getters and Setters
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
    
    public PipelineStep getFirstStep() {
        return firstStep;
    }
    
    @Override
    public String toString() {
        return "PipelineChain{" +
                "chainId='" + chainId + '\'' +
                ", chainName='" + chainName + '\'' +
                ", stepCount=" + steps.size() +
                ", built=" + built +
                '}';
    }
} 