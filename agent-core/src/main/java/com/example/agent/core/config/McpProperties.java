package com.example.agent.core.config;

import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;

/**
 * MCP协议配置属性
 * 
 * @author agent
 */
@Validated
public class McpProperties {

    /**
     * 是否启用MCP协议
     */
    private boolean enabled = true;

    /**
     * WebSocket配置
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private WebSocketProperties websocket = new WebSocketProperties();

    /**
     * JSON-RPC配置
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private JsonRpcProperties jsonRpc = new JsonRpcProperties();

    /**
     * 工具管理配置
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private ToolProperties tool = new ToolProperties();

    /**
     * 资源管理配置
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private ResourceProperties resource = new ResourceProperties();

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public WebSocketProperties getWebsocket() {
        return websocket;
    }

    public void setWebsocket(WebSocketProperties websocket) {
        this.websocket = websocket;
    }

    public JsonRpcProperties getJsonRpc() {
        return jsonRpc;
    }

    public void setJsonRpc(JsonRpcProperties jsonRpc) {
        this.jsonRpc = jsonRpc;
    }

    public ToolProperties getTool() {
        return tool;
    }

    public void setTool(ToolProperties tool) {
        this.tool = tool;
    }

    public ResourceProperties getResource() {
        return resource;
    }

    public void setResource(ResourceProperties resource) {
        this.resource = resource;
    }

    /**
     * WebSocket配置
     */
    @Validated
    public static class WebSocketProperties {
        /**
         * WebSocket服务端口
         */
        @Min(1024)
        @Max(65535)
        private int port = 8081;

        /**
         * WebSocket路径
         */
        @NotBlank
        private String path = "/mcp";

        /**
         * 最大连接数
         */
        @Positive
        private int maxConnections = 100;

        /**
         * 连接超时时间
         */
        @NotNull
        private Duration connectionTimeout = Duration.ofSeconds(30);

        /**
         * 心跳间隔
         */
        @NotNull
        private Duration heartbeatInterval = Duration.ofSeconds(30);

        /**
         * 消息最大大小
         */
        @Positive
        private int maxMessageSize = 1048576; // 1MB

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public int getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }

        public Duration getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(Duration connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public Duration getHeartbeatInterval() {
            return heartbeatInterval;
        }

        public void setHeartbeatInterval(Duration heartbeatInterval) {
            this.heartbeatInterval = heartbeatInterval;
        }

        public int getMaxMessageSize() {
            return maxMessageSize;
        }

        public void setMaxMessageSize(int maxMessageSize) {
            this.maxMessageSize = maxMessageSize;
        }

        @Override
        public String toString() {
            return "WebSocketProperties{" +
                    "port=" + port +
                    ", path='" + path + '\'' +
                    ", maxConnections=" + maxConnections +
                    ", connectionTimeout=" + connectionTimeout +
                    ", heartbeatInterval=" + heartbeatInterval +
                    ", maxMessageSize=" + maxMessageSize +
                    '}';
        }
    }

    /**
     * JSON-RPC配置
     */
    @Validated
    public static class JsonRpcProperties {
        /**
         * 协议版本
         */
        @NotBlank
        private String version = "2.0";

        /**
         * 请求超时时间
         */
        @NotNull
        private Duration requestTimeout = Duration.ofSeconds(30);

        /**
         * 最大批处理大小
         */
        @Positive
        private int maxBatchSize = 100;

        /**
         * 是否启用批处理
         */
        private boolean batchEnabled = true;

        /**
         * 是否启用通知（单向调用）
         */
        private boolean notificationEnabled = true;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public Duration getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
        }

        public int getMaxBatchSize() {
            return maxBatchSize;
        }

        public void setMaxBatchSize(int maxBatchSize) {
            this.maxBatchSize = maxBatchSize;
        }

        public boolean isBatchEnabled() {
            return batchEnabled;
        }

        public void setBatchEnabled(boolean batchEnabled) {
            this.batchEnabled = batchEnabled;
        }

        public boolean isNotificationEnabled() {
            return notificationEnabled;
        }

        public void setNotificationEnabled(boolean notificationEnabled) {
            this.notificationEnabled = notificationEnabled;
        }

        @Override
        public String toString() {
            return "JsonRpcProperties{" +
                    "version='" + version + '\'' +
                    ", requestTimeout=" + requestTimeout +
                    ", maxBatchSize=" + maxBatchSize +
                    ", batchEnabled=" + batchEnabled +
                    ", notificationEnabled=" + notificationEnabled +
                    '}';
        }
    }

    /**
     * 工具管理配置
     */
    @Validated
    public static class ToolProperties {
        /**
         * 工具发现超时时间
         */
        @NotNull
        private Duration discoveryTimeout = Duration.ofSeconds(10);

        /**
         * 工具执行超时时间
         */
        @NotNull
        private Duration executionTimeout = Duration.ofSeconds(60);

        /**
         * 最大工具数量
         */
        @Positive
        private int maxTools = 100;

        /**
         * 是否启用工具验证
         */
        private boolean validationEnabled = true;

        /**
         * 是否启用权限检查
         */
        private boolean permissionCheckEnabled = true;

        public Duration getDiscoveryTimeout() {
            return discoveryTimeout;
        }

        public void setDiscoveryTimeout(Duration discoveryTimeout) {
            this.discoveryTimeout = discoveryTimeout;
        }

        public Duration getExecutionTimeout() {
            return executionTimeout;
        }

        public void setExecutionTimeout(Duration executionTimeout) {
            this.executionTimeout = executionTimeout;
        }

        public int getMaxTools() {
            return maxTools;
        }

        public void setMaxTools(int maxTools) {
            this.maxTools = maxTools;
        }

        public boolean isValidationEnabled() {
            return validationEnabled;
        }

        public void setValidationEnabled(boolean validationEnabled) {
            this.validationEnabled = validationEnabled;
        }

        public boolean isPermissionCheckEnabled() {
            return permissionCheckEnabled;
        }

        public void setPermissionCheckEnabled(boolean permissionCheckEnabled) {
            this.permissionCheckEnabled = permissionCheckEnabled;
        }

        @Override
        public String toString() {
            return "ToolProperties{" +
                    "discoveryTimeout=" + discoveryTimeout +
                    ", executionTimeout=" + executionTimeout +
                    ", maxTools=" + maxTools +
                    ", validationEnabled=" + validationEnabled +
                    ", permissionCheckEnabled=" + permissionCheckEnabled +
                    '}';
        }
    }

    /**
     * 资源管理配置
     */
    @Validated
    public static class ResourceProperties {
        /**
         * 资源访问超时时间
         */
        @NotNull
        private Duration accessTimeout = Duration.ofSeconds(30);

        /**
         * 最大资源数量
         */
        @Positive
        private int maxResources = 1000;

        /**
         * 资源缓存大小
         */
        @Positive
        private int cacheSize = 100;

        /**
         * 缓存存活时间
         */
        @NotNull
        private Duration cacheTtl = Duration.ofMinutes(10);

        public Duration getAccessTimeout() {
            return accessTimeout;
        }

        public void setAccessTimeout(Duration accessTimeout) {
            this.accessTimeout = accessTimeout;
        }

        public int getMaxResources() {
            return maxResources;
        }

        public void setMaxResources(int maxResources) {
            this.maxResources = maxResources;
        }

        public int getCacheSize() {
            return cacheSize;
        }

        public void setCacheSize(int cacheSize) {
            this.cacheSize = cacheSize;
        }

        public Duration getCacheTtl() {
            return cacheTtl;
        }

        public void setCacheTtl(Duration cacheTtl) {
            this.cacheTtl = cacheTtl;
        }

        @Override
        public String toString() {
            return "ResourceProperties{" +
                    "accessTimeout=" + accessTimeout +
                    ", maxResources=" + maxResources +
                    ", cacheSize=" + cacheSize +
                    ", cacheTtl=" + cacheTtl +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "McpProperties{" +
                "enabled=" + enabled +
                ", websocket=" + websocket +
                ", jsonRpc=" + jsonRpc +
                ", tool=" + tool +
                ", resource=" + resource +
                '}';
    }
} 