package com.example.agent.app.config;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 敏感配置信息处理器
 * 处理API密钥等敏感配置的加密、掩码和安全存储
 * 
 * @author agent
 */
@Component
public class SensitiveConfigurationHandler {

    private final Environment environment;
    
    // 敏感配置项模式
    private static final List<Pattern> SENSITIVE_PATTERNS = Arrays.asList(
        Pattern.compile(".*\\.key$", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.password$", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.secret$", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.token$", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*api[_-]?key.*", Pattern.CASE_INSENSITIVE)
    );

    public SensitiveConfigurationHandler(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void checkSensitiveConfiguration() {
        checkEnvironmentVariables();
        provideSecurityRecommendations();
    }

    /**
     * 检查环境变量配置
     */
    private void checkEnvironmentVariables() {
        // 检查关键的API密钥是否通过环境变量配置
        String openaiKey = environment.getProperty("OPENAI_API_KEY");
        String claudeKey = environment.getProperty("CLAUDE_API_KEY");
        
        if (!StringUtils.hasText(openaiKey) && !StringUtils.hasText(claudeKey)) {
            System.out.println("⚠️  警告: 未检测到API密钥环境变量，请确保正确配置 OPENAI_API_KEY 或 CLAUDE_API_KEY");
        }
    }

    /**
     * 提供安全建议
     */
    private void provideSecurityRecommendations() {
        System.out.println("\n🔒 敏感配置安全建议:");
        System.out.println("1. 使用环境变量存储API密钥，避免在配置文件中硬编码");
        System.out.println("2. 在生产环境中启用配置加密");
        System.out.println("3. 定期轮换API密钥");
        System.out.println("4. 限制配置文件的访问权限");
        System.out.println("5. 使用密钥管理服务（如AWS KMS、Azure Key Vault）");
    }

    /**
     * 掩码敏感信息用于日志输出
     */
    public String maskSensitiveValue(String key, String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        
        if (isSensitiveKey(key)) {
            return maskValue(value);
        }
        
        return value;
    }

    /**
     * 判断是否为敏感配置键
     */
    public boolean isSensitiveKey(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        
        return SENSITIVE_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(key).matches());
    }

    /**
     * 掩码值
     */
    private String maskValue(String value) {
        if (value.length() <= 4) {
            return "***";
        }
        
        if (value.length() <= 8) {
            return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
        }
        
        return value.substring(0, 4) + "***" + value.substring(value.length() - 4);
    }

    /**
     * 简单的Base64编码（注意：这不是加密，只是编码）
     */
    public String encodeValue(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return Base64.getEncoder().encodeToString(value.getBytes());
    }

    /**
     * 简单的Base64解码
     */
    public String decodeValue(String encodedValue) {
        if (!StringUtils.hasText(encodedValue)) {
            return encodedValue;
        }
        try {
            return new String(Base64.getDecoder().decode(encodedValue));
        } catch (IllegalArgumentException e) {
            return encodedValue; // 如果不是有效的Base64，返回原值
        }
    }

    /**
     * 验证API密钥格式
     */
    public boolean isValidApiKeyFormat(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return false;
        }
        
        // OpenAI API密钥格式验证（以sk-开头）
        if (apiKey.startsWith("sk-") && apiKey.length() > 20) {
            return true;
        }
        
        // Claude API密钥格式验证（以sk-ant-开头）
        if (apiKey.startsWith("sk-ant-") && apiKey.length() > 30) {
            return true;
        }
        
        // 通用API密钥长度验证
        return apiKey.length() >= 16 && apiKey.length() <= 200;
    }

    /**
     * 生成配置模板
     */
    public void generateConfigurationTemplate() {
        System.out.println("\n📝 环境变量配置模板:");
        System.out.println("# API密钥配置");
        System.out.println("export OPENAI_API_KEY=your-openai-api-key");
        System.out.println("export CLAUDE_API_KEY=your-claude-api-key");
        System.out.println("\n# 可选配置");
        System.out.println("export AGENT_DEBUG_MODE=false");
        System.out.println("export AGENT_LOG_LEVEL=INFO");
        
        System.out.println("\n🐳 Docker环境变量配置:");
        System.out.println("docker run -e OPENAI_API_KEY=your-key -e CLAUDE_API_KEY=your-key ...");
        
        System.out.println("\n☸️  Kubernetes Secret配置:");
        System.out.println("kubectl create secret generic agent-secrets \\");
        System.out.println("  --from-literal=openai-api-key=your-key \\");
        System.out.println("  --from-literal=claude-api-key=your-key");
    }

    /**
     * 配置健康检查
     */
    public ConfigurationHealth checkConfigurationHealth() {
        ConfigurationHealth health = new ConfigurationHealth();
        
        // 检查API密钥配置
        String openaiKey = environment.getProperty("agent.llm.api.openai.key");
        String claudeKey = environment.getProperty("agent.llm.api.claude.key");
        
        health.setOpenaiConfigured(StringUtils.hasText(openaiKey) && isValidApiKeyFormat(openaiKey));
        health.setClaudeConfigured(StringUtils.hasText(claudeKey) && isValidApiKeyFormat(claudeKey));
        health.setHasValidConfiguration(health.isOpenaiConfigured() || health.isClaudeConfigured());
        
        return health;
    }

    /**
     * 配置健康状态
     */
    public static class ConfigurationHealth {
        private boolean openaiConfigured;
        private boolean claudeConfigured;
        private boolean hasValidConfiguration;

        public boolean isOpenaiConfigured() {
            return openaiConfigured;
        }

        public void setOpenaiConfigured(boolean openaiConfigured) {
            this.openaiConfigured = openaiConfigured;
        }

        public boolean isClaudeConfigured() {
            return claudeConfigured;
        }

        public void setClaudeConfigured(boolean claudeConfigured) {
            this.claudeConfigured = claudeConfigured;
        }

        public boolean isHasValidConfiguration() {
            return hasValidConfiguration;
        }

        public void setHasValidConfiguration(boolean hasValidConfiguration) {
            this.hasValidConfiguration = hasValidConfiguration;
        }

        @Override
        public String toString() {
            return "ConfigurationHealth{" +
                    "openaiConfigured=" + openaiConfigured +
                    ", claudeConfigured=" + claudeConfigured +
                    ", hasValidConfiguration=" + hasValidConfiguration +
                    '}';
        }
    }
} 