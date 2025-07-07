package com.example.agent.app.config;

import com.example.agent.core.config.AgentProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Agent配置管理器
 * 启用配置属性并提供配置验证功能
 * 
 * @author agent
 */
@Configuration
@EnableConfigurationProperties(AgentProperties.class)
@ConfigurationPropertiesScan("com.example.agent.core.config")
public class AgentConfigurationManager {

    /**
     * Bean验证器工厂
     */
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    /**
     * 配置验证服务
     */
    @Bean
    public ConfigurationValidationService configurationValidationService(AgentProperties agentProperties, 
                                                                         LocalValidatorFactoryBean validator) {
        return new ConfigurationValidationService(agentProperties, validator);
    }
} 