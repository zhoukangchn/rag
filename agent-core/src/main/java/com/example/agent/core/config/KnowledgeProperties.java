package com.example.agent.core.config;

import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;

/**
 * 知识源配置属性
 * 
 * @author agent
 */
@Validated
public class KnowledgeProperties {

    /**
     * 向量存储配置
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private VectorStoreProperties vectorStore = new VectorStoreProperties();

    /**
     * SQL数据库配置
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private SqlDatabaseProperties sqlDatabase = new SqlDatabaseProperties();

    /**
     * API数据源配置
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private ApiSourceProperties apiSource = new ApiSourceProperties();

    // Getters and Setters
    public VectorStoreProperties getVectorStore() {
        return vectorStore;
    }

    public void setVectorStore(VectorStoreProperties vectorStore) {
        this.vectorStore = vectorStore;
    }

    public SqlDatabaseProperties getSqlDatabase() {
        return sqlDatabase;
    }

    public void setSqlDatabase(SqlDatabaseProperties sqlDatabase) {
        this.sqlDatabase = sqlDatabase;
    }

    public ApiSourceProperties getApiSource() {
        return apiSource;
    }

    public void setApiSource(ApiSourceProperties apiSource) {
        this.apiSource = apiSource;
    }

    /**
     * 向量存储配置
     */
    @Validated
    public static class VectorStoreProperties {
        /**
         * 是否启用向量存储
         */
        private boolean enabled = true;

        /**
         * 向量维度
         */
        @Positive
        private int dimension = 1536;

        /**
         * 相似度阈值
         */
        private double similarityThreshold = 0.7;

        /**
         * 最大检索结果数
         */
        @Positive
        private int maxResults = 10;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getDimension() {
            return dimension;
        }

        public void setDimension(int dimension) {
            this.dimension = dimension;
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

        @Override
        public String toString() {
            return "VectorStoreProperties{" +
                    "enabled=" + enabled +
                    ", dimension=" + dimension +
                    ", similarityThreshold=" + similarityThreshold +
                    ", maxResults=" + maxResults +
                    '}';
        }
    }

    /**
     * SQL数据库配置
     */
    @Validated
    public static class SqlDatabaseProperties {
        /**
         * 是否启用SQL数据库
         */
        private boolean enabled = true;

        /**
         * 查询超时时间
         */
        @NotNull
        private Duration queryTimeout = Duration.ofSeconds(30);

        /**
         * 最大连接数
         */
        @Positive
        private int maxConnections = 10;

        /**
         * 最大检索结果数
         */
        @Positive
        private int maxResults = 100;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getQueryTimeout() {
            return queryTimeout;
        }

        public void setQueryTimeout(Duration queryTimeout) {
            this.queryTimeout = queryTimeout;
        }

        public int getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }

        public int getMaxResults() {
            return maxResults;
        }

        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }

        @Override
        public String toString() {
            return "SqlDatabaseProperties{" +
                    "enabled=" + enabled +
                    ", queryTimeout=" + queryTimeout +
                    ", maxConnections=" + maxConnections +
                    ", maxResults=" + maxResults +
                    '}';
        }
    }

    /**
     * API数据源配置
     */
    @Validated
    public static class ApiSourceProperties {
        /**
         * 是否启用API数据源
         */
        private boolean enabled = true;

        /**
         * API调用超时时间
         */
        @NotNull
        private Duration timeout = Duration.ofSeconds(30);

        /**
         * 最大并发请求数
         */
        @Positive
        private int maxConcurrentRequests = 5;

        /**
         * 最大检索结果数
         */
        @Positive
        private int maxResults = 50;

        /**
         * 重试次数
         */
        @Positive
        private int retryAttempts = 3;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public int getMaxConcurrentRequests() {
            return maxConcurrentRequests;
        }

        public void setMaxConcurrentRequests(int maxConcurrentRequests) {
            this.maxConcurrentRequests = maxConcurrentRequests;
        }

        public int getMaxResults() {
            return maxResults;
        }

        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }

        public int getRetryAttempts() {
            return retryAttempts;
        }

        public void setRetryAttempts(int retryAttempts) {
            this.retryAttempts = retryAttempts;
        }

        @Override
        public String toString() {
            return "ApiSourceProperties{" +
                    "enabled=" + enabled +
                    ", timeout=" + timeout +
                    ", maxConcurrentRequests=" + maxConcurrentRequests +
                    ", maxResults=" + maxResults +
                    ", retryAttempts=" + retryAttempts +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "KnowledgeProperties{" +
                "vectorStore=" + vectorStore +
                ", sqlDatabase=" + sqlDatabase +
                ", apiSource=" + apiSource +
                '}';
    }
} 