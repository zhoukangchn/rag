package com.example.agent.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Agent主配置属性类
 * 
 * @author agent
 */
@ConfigurationProperties(prefix = "agent")
@Validated
public class AgentProperties {

    /**
     * LLM客户端配置
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private LlmProperties llm = new LlmProperties();

    /**
     * 知识源配置
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private KnowledgeProperties knowledge = new KnowledgeProperties();

    /**
     * 处理管道配置
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private PipelineProperties pipeline = new PipelineProperties();

    /**
     * MCP协议配置
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private McpProperties mcp = new McpProperties();

    // Getters and Setters
    public LlmProperties getLlm() {
        return llm;
    }

    public void setLlm(LlmProperties llm) {
        this.llm = llm;
    }

    public KnowledgeProperties getKnowledge() {
        return knowledge;
    }

    public void setKnowledge(KnowledgeProperties knowledge) {
        this.knowledge = knowledge;
    }

    public PipelineProperties getPipeline() {
        return pipeline;
    }

    public void setPipeline(PipelineProperties pipeline) {
        this.pipeline = pipeline;
    }

    public McpProperties getMcp() {
        return mcp;
    }

    public void setMcp(McpProperties mcp) {
        this.mcp = mcp;
    }

    @Override
    public String toString() {
        return "AgentProperties{" +
                "llm=" + llm +
                ", knowledge=" + knowledge +
                ", pipeline=" + pipeline +
                ", mcp=" + mcp +
                '}';
    }
} 