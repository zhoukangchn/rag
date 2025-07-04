package com.example.agent.knowledge.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.reactive.function.client.WebClient;

import javax.sql.DataSource;
import java.time.Duration;

/**
 * 数据源配置类
 * 
 * @author agent
 */
@Configuration
@EnableTransactionManagement
public class DataSourceConfig {
    
    /**
     * 主数据源（H2数据库）
     */
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public DataSource primaryDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:agent_knowledge;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("org.h2.Driver");
        
        // 连接池配置
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        
        // 连接验证
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        
        // 性能优化
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        
        return new HikariDataSource(config);
    }
    
    /**
     * 向量数据库数据源（H2 for development）
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.vector")
    public DataSource vectorDataSource() {
        return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:vector_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                .username("sa")
                .password("")
                .build();
    }
    
    /**
     * 主数据源JdbcTemplate
     */
    @Bean
    @Primary
    public JdbcTemplate primaryJdbcTemplate(@Qualifier("primaryDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
    
    /**
     * 向量数据库JdbcTemplate
     */
    @Bean
    public JdbcTemplate vectorJdbcTemplate(@Qualifier("vectorDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
    
    /**
     * 主数据源事务管理器
     */
    @Bean
    @Primary
    public PlatformTransactionManager primaryTransactionManager(@Qualifier("primaryDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
    
    /**
     * 向量数据库事务管理器
     */
    @Bean
    public PlatformTransactionManager vectorTransactionManager(@Qualifier("vectorDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
    
    /**
     * WebClient配置（用于API调用）
     */
    @Bean
    @Primary
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();
    }
    
    /**
     * API客户端WebClient（带超时配置）
     */
    @Bean
    public WebClient apiWebClient() {
        return WebClient.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(32 * 1024 * 1024); // 32MB
                    configurer.defaultCodecs().enableLoggingRequestDetails(true);
                })
                .build();
    }
    
    /**
     * 向量存储配置
     */
    @Bean
    @ConfigurationProperties(prefix = "agent.knowledge.vector-store")
    public VectorStoreProperties vectorStoreProperties() {
        return new VectorStoreProperties();
    }
    
    /**
     * API源配置
     */
    @Bean
    @ConfigurationProperties(prefix = "agent.knowledge.api-sources")
    public ApiSourceProperties apiSourceProperties() {
        return new ApiSourceProperties();
    }
    
    /**
     * 知识库配置
     */
    @Bean
    @ConfigurationProperties(prefix = "agent.knowledge")
    public KnowledgeConfig knowledgeConfig() {
        return new KnowledgeConfig();
    }
    
    /**
     * 向量存储属性配置类
     */
    public static class VectorStoreProperties {
        private String type = "in-memory"; // in-memory, postgresql, elasticsearch, etc.
        private String host = "localhost";
        private int port = 5432;
        private String database = "vector_db";
        private String username;
        private String password;
        private int dimensions = 768; // 默认向量维度
        private String indexType = "ivfflat"; // 索引类型
        private double similarityThreshold = 0.7; // 相似度阈值
        private int maxResults = 10; // 最大返回结果数
        private boolean enableCache = true; // 是否启用缓存
        
        // Getters and Setters
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getHost() {
            return host;
        }
        
        public void setHost(String host) {
            this.host = host;
        }
        
        public int getPort() {
            return port;
        }
        
        public void setPort(int port) {
            this.port = port;
        }
        
        public String getDatabase() {
            return database;
        }
        
        public void setDatabase(String database) {
            this.database = database;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public int getDimensions() {
            return dimensions;
        }
        
        public void setDimensions(int dimensions) {
            this.dimensions = dimensions;
        }
        
        public String getIndexType() {
            return indexType;
        }
        
        public void setIndexType(String indexType) {
            this.indexType = indexType;
        }
        
        public double getSimilarityThreshold() {
            return similarityThreshold;
        }
        
        public void setSimilarityThreshold(double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
        }
        
        public int getMaxResults() {
            return maxResults;
        }
        
        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }
        
        public boolean isEnableCache() {
            return enableCache;
        }
        
        public void setEnableCache(boolean enableCache) {
            this.enableCache = enableCache;
        }
    }
    
    /**
     * API源属性配置类
     */
    public static class ApiSourceProperties {
        private int defaultTimeout = 30000; // 默认超时时间（毫秒）
        private int maxRetries = 3; // 最大重试次数
        private int rateLimitPerMinute = 60; // 每分钟请求限制
        private boolean enableCircuitBreaker = true; // 启用熔断器
        private String userAgent = "Agent-Knowledge-System/1.0";
        
        // Getters and Setters
        public int getDefaultTimeout() {
            return defaultTimeout;
        }
        
        public void setDefaultTimeout(int defaultTimeout) {
            this.defaultTimeout = defaultTimeout;
        }
        
        public int getMaxRetries() {
            return maxRetries;
        }
        
        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }
        
        public int getRateLimitPerMinute() {
            return rateLimitPerMinute;
        }
        
        public void setRateLimitPerMinute(int rateLimitPerMinute) {
            this.rateLimitPerMinute = rateLimitPerMinute;
        }
        
        public boolean isEnableCircuitBreaker() {
            return enableCircuitBreaker;
        }
        
        public void setEnableCircuitBreaker(boolean enableCircuitBreaker) {
            this.enableCircuitBreaker = enableCircuitBreaker;
        }
        
        public String getUserAgent() {
            return userAgent;
        }
        
        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }
    }
    
    /**
     * 知识库通用配置类
     */
    public static class KnowledgeConfig {
        private boolean enableAsync = true; // 启用异步处理
        private int asyncThreadPoolSize = 10; // 异步线程池大小
        private long cacheTimeoutMinutes = 30; // 缓存超时时间（分钟）
        private int batchSize = 100; // 批处理大小
        private boolean enableMetrics = true; // 启用指标收集
        private String defaultEncoding = "UTF-8";
        
        // Getters and Setters
        public boolean isEnableAsync() {
            return enableAsync;
        }
        
        public void setEnableAsync(boolean enableAsync) {
            this.enableAsync = enableAsync;
        }
        
        public int getAsyncThreadPoolSize() {
            return asyncThreadPoolSize;
        }
        
        public void setAsyncThreadPoolSize(int asyncThreadPoolSize) {
            this.asyncThreadPoolSize = asyncThreadPoolSize;
        }
        
        public long getCacheTimeoutMinutes() {
            return cacheTimeoutMinutes;
        }
        
        public void setCacheTimeoutMinutes(long cacheTimeoutMinutes) {
            this.cacheTimeoutMinutes = cacheTimeoutMinutes;
        }
        
        public int getBatchSize() {
            return batchSize;
        }
        
        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
        
        public boolean isEnableMetrics() {
            return enableMetrics;
        }
        
        public void setEnableMetrics(boolean enableMetrics) {
            this.enableMetrics = enableMetrics;
        }
        
        public String getDefaultEncoding() {
            return defaultEncoding;
        }
        
        public void setDefaultEncoding(String defaultEncoding) {
            this.defaultEncoding = defaultEncoding;
        }
    }
} 