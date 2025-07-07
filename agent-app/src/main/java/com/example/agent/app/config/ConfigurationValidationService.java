package com.example.agent.app.config;

import com.example.agent.core.config.AgentProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 配置验证服务
 * 提供配置属性的验证和业务规则检查
 * 
 * @author agent
 */
@Service
public class ConfigurationValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationValidationService.class);

    private final AgentProperties agentProperties;
    private final Validator validator;

    public ConfigurationValidationService(AgentProperties agentProperties, Validator validator) {
        this.agentProperties = agentProperties;
        this.validator = validator;
    }

    /**
     * 启动时验证配置
     */
    @PostConstruct
    public void validateConfiguration() {
        logger.info("开始验证Agent配置...");
        
        List<String> validationErrors = new ArrayList<>();
        
        // 基本验证
        Set<ConstraintViolation<AgentProperties>> violations = validator.validate(agentProperties);
        for (ConstraintViolation<AgentProperties> violation : violations) {
            validationErrors.add(violation.getPropertyPath() + ": " + violation.getMessage());
        }
        
        // 业务规则验证
        validateBusinessRules(validationErrors);
        
        if (!validationErrors.isEmpty()) {
            logger.error("配置验证失败:");
            validationErrors.forEach(error -> logger.error("  - {}", error));
            throw new IllegalStateException("配置验证失败，请检查配置文件");
        }
        
        logger.info("Agent配置验证通过");
        logConfigurationSummary();
    }

    /**
     * 验证业务规则
     */
    private void validateBusinessRules(List<String> errors) {
        // LLM配置验证
        validateLlmConfiguration(errors);
        
        // 知识源配置验证
        validateKnowledgeConfiguration(errors);
        
        // 管道配置验证
        validatePipelineConfiguration(errors);
        
        // MCP配置验证
        validateMcpConfiguration(errors);
    }

    /**
     * 验证LLM配置
     */
    private void validateLlmConfiguration(List<String> errors) {
        var llm = agentProperties.getLlm();
        
        // 验证重试配置合理性
        if (llm.getRetry().getMaxAttempts() > 5 && llm.getRetry().getDelay() < 500) {
            errors.add("LLM重试配置: 重试次数过多但延迟过短，可能导致频繁请求");
        }
    }

    /**
     * 验证知识源配置
     */
    private void validateKnowledgeConfiguration(List<String> errors) {
        var knowledge = agentProperties.getKnowledge();
        
        // 检查至少启用一个知识源
        if (!knowledge.getVectorStore().isEnabled() && 
            !knowledge.getSqlDatabase().isEnabled() && 
            !knowledge.getApiSource().isEnabled()) {
            errors.add("知识源配置: 至少需要启用一个知识源");
        }
        
        // 验证向量存储维度合理性
        if (knowledge.getVectorStore().isEnabled()) {
            int dimension = knowledge.getVectorStore().getDimension();
            if (dimension < 100 || dimension > 4096) {
                errors.add("向量存储配置: 向量维度应该在100-4096之间，当前值: " + dimension);
            }
        }
    }

    /**
     * 验证管道配置
     */
    private void validatePipelineConfiguration(List<String> errors) {
        var pipeline = agentProperties.getPipeline();
        
        // 验证线程池配置合理性
        if (pipeline.getCorePoolSize() > pipeline.getMaxPoolSize()) {
            errors.add("管道配置: 核心线程数不能大于最大线程数");
        }
        
        if (pipeline.getMaxConcurrency() > pipeline.getMaxPoolSize() * 2) {
            errors.add("管道配置: 最大并发数过高，建议不超过最大线程数的2倍");
        }
        
        // 验证超时配置
        if (pipeline.getTimeout().toSeconds() < 10) {
            errors.add("管道配置: 处理超时时间过短，建议至少10秒");
        }
    }

    /**
     * 验证MCP配置
     */
    private void validateMcpConfiguration(List<String> errors) {
        var mcp = agentProperties.getMcp();
        
        if (mcp.isEnabled()) {
            // 验证WebSocket端口不冲突
            var websocket = mcp.getWebsocket();
            if (websocket.getPort() == 8080) {
                errors.add("MCP配置: WebSocket端口不能与应用端口相同");
            }
            
            // 验证连接数限制合理性
            if (websocket.getMaxConnections() > 1000) {
                errors.add("MCP配置: 最大连接数过高，建议不超过1000");
            }
        }
    }

    /**
     * 记录配置摘要
     */
    private void logConfigurationSummary() {
        logger.info("=== Agent配置摘要 ===");
        logger.info("LLM配置: {}", agentProperties.getLlm());
        logger.info("知识源配置: {}", agentProperties.getKnowledge());
        logger.info("管道配置: {}", agentProperties.getPipeline());
        logger.info("MCP配置: {}", agentProperties.getMcp());
        logger.info("===================");
    }

    /**
     * 运行时验证配置变更
     */
    public boolean validateConfigurationChange(AgentProperties newProperties) {
        Set<ConstraintViolation<AgentProperties>> violations = validator.validate(newProperties);
        if (!violations.isEmpty()) {
            logger.warn("配置变更验证失败: {}", violations);
            return false;
        }
        
        List<String> errors = new ArrayList<>();
        // 这里可以添加运行时特定的验证规则
        
        return errors.isEmpty();
    }
} 