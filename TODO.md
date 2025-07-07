# Spring Boot Knowledge Agent - TODO 任务清单

## 📋 项目概述
基于设计文档（ragAgentPrd.md）实现Spring Boot知识Agent，采用领域驱动的多模块架构，支持责任链处理、多源知识检索、双向流式处理和MCP协议。

## 🎯 任务进度统计
- 总任务数：31
- 已完成：16/31 (51.6%)
- 进行中：0/31 (0%)
- 待开始：15/31 (48.4%)

---

## 📝 任务详细列表

### 阶段1：基础架构搭建 (1-7) ✅ 已完成

#### ✅ 1. project-restructure
- **状态**: 已完成
- **描述**: 重构项目为多模块Maven架构
- **完成内容**:
  - 创建父级pom.xml配置
  - 创建5个子模块：agent-core、agent-knowledge、agent-pipeline、agent-mcp、agent-app
  - 配置模块间依赖关系
  - 迁移现有代码到对应模块

#### ✅ 2. maven-parent-pom  
- **状态**: 已完成
- **描述**: 配置父级pom.xml和依赖管理
- **完成内容**:
  - Spring Boot依赖管理
  - 多环境配置（dev/test/prod）
  - JaCoCo测试覆盖率配置
  - Jib Docker插件配置

#### ✅ 3. agent-core-module
- **状态**: 已完成
- **描述**: 实现核心基础模块
- **完成内容**:
  - 创建 dto、exception、util、constant、step、strategy 等包
  - 创建 DTO 类：AgentContext、KnowledgeChunk、PlanDetail
  - 定义核心接口：PipelineStep、KnowledgeSourceStrategy
  - 实现自定义异常类 AgentException
  - 创建工具类 AgentUtils 和常量类 AgentConstants
- **预计工期**: 2天
- **依赖**: project-restructure

#### ✅ 4. agent-knowledge-structure
- **状态**: 已完成
- **描述**: 搭建知识管理模块基础结构
- **完成内容**:
  - 创建 strategy、factory、repository、config 等包
  - 创建策略实现骨架：VectorStoreStrategyImpl、SqlDatabaseStrategyImpl、ApiSourceStrategyImpl
  - 设计知识源工厂类 KnowledgeSourceFactory
  - 定义数据访问仓库接口：VectorStoreRepository、SqlDatabaseRepository、ApiSourceRepository
  - 配置数据源连接骨架 DataSourceConfig
- **预计工期**: 2天
- **依赖**: agent-core-module

#### ✅ 5. agent-pipeline-structure
- **状态**: 已完成
- **描述**: 搭建处理流程模块基础结构
- **完成内容**:
  - 创建 chain、template、service、config、step 等包
  - 创建责任链基础框架：PipelineChain、ChainContext
  - 定义流程步骤模板：AbstractPipelineStep
  - 实现外观服务类：AgentPipelineService
  - 配置Spring组件扫描：PipelineConfig
  - 创建具体步骤骨架：PlanningStep、KnowledgeRecallStep、PromptConstructionStep、ModelInvocationStep
- **预计工期**: 2天
- **依赖**: agent-core-module

#### ✅ 6. agent-mcp-structure
- **状态**: 已完成
- **描述**: 搭建MCP协议模块基础结构
- **完成内容**:
  - 创建 tool、protocol、websocket、jsonrpc、server、config、dto 等包
  - 定义MCP工具接口：McpTool、ToolRegistry
  - 创建协议处理器：McpProtocolHandler
  - 实现WebSocket端点：McpWebSocketEndpoint
  - 配置JSON-RPC处理：JsonRpcProcessor
  - 创建MCP服务器：McpServer
  - 定义DTO结构：McpRequest、McpResponse
  - 配置模块：McpConfig
- **预计工期**: 1.5天
- **依赖**: agent-core-module

#### ✅ 7. agent-app-config
- **状态**: 已完成
- **描述**: 配置应用模块和启动类
- **完成内容**:
  - 完善 AgentApplication 启动类，添加组件扫描、异步支持、事务管理
  - 创建完整的 application.yml 主配置文件（替换原 application.properties）
  - 配置多环境文件：application-dev.yml、application-prod.yml
  - 创建 logback-spring.xml 日志配置，支持多环境和文件轮转
  - 配置 Spring Boot Actuator 监控端点
  - 添加自定义 Agent 配置项
- **预计工期**: 1天
- **依赖**: 模块3-6全部完成

### 阶段2：核心功能模块 (8-16) ✅ 已完成

#### ✅ 8. knowledge-repository
- **状态**: 已完成
- **描述**: 实现知识检索仓库层
- **完成内容**:
  - ✅ 完善KnowledgeChunk DTO，添加所有必要属性
  - ✅ 实现VectorStoreRepository接口和实现类，支持向量相似度搜索
  - ✅ 实现SqlDatabaseRepository接口和实现类，支持SQL查询操作
  - ✅ 实现ApiSourceRepository接口和实现类，支持外部API调用
  - ✅ 配置DataSourceConfig，包含连接池、事务管理和多数据源
  - ✅ 创建所有仓库的实现类：VectorStoreRepositoryImpl、SqlDatabaseRepositoryImpl、ApiSourceRepositoryImpl
- **预计工期**: 3天
- **依赖**: agent-knowledge-structure

#### ✅ 9. knowledge-strategy-impl
- **状态**: 已完成
- **描述**: 实现具体的知识检索策略
- **完成内容**:
  - ✅ VectorStoreStrategyImpl：基于向量相似度的知识检索，支持语义搜索和相似度匹配
  - ✅ SqlDatabaseStrategyImpl：基于SQL查询的知识检索，支持关键词搜索和条件查询
  - ✅ ApiSourceStrategyImpl：基于外部API的知识检索，支持异步调用和多源聚合
  - ✅ KnowledgeSourceFactory：智能策略选择工厂，支持上下文感知的策略选择
  - ✅ KnowledgeSourceStrategy接口：统一的策略接口定义
  - ✅ AgentContext DTO：完善的上下文信息管理
- **预计工期**: 3天
- **依赖**: knowledge-repository

#### ✅ 10. pipeline-steps-impl
- **状态**: 已完成
- **描述**: 实现责任链处理步骤
- **完成内容**:
  - ✅ 完善PipelineStep接口，定义处理方法和支持责任链模式
  - ✅ 实现AbstractPipelineStep抽象类，包含责任链模式的基本功能和模板方法
  - ✅ 完善PlanDetail DTO，添加计划详情属性
  - ✅ 实现PlanningStep：查询规划，包含查询分析、意图识别、策略选择等
  - ✅ 实现KnowledgeRecallStep：知识召回，支持多策略并行检索和后处理
  - ✅ 实现PromptConstructionStep：prompt构建，将知识块组装成LLM提示
  - ✅ 实现ModelInvocationStep：模型调用，调用LLM生成最终响应
  - ✅ 完善KnowledgeChunk DTO，添加retrievalTime和score属性
  - ✅ 完善KnowledgeSourceStrategy接口，添加简化的retrieve方法
- **预计工期**: 4天
- **依赖**: agent-pipeline-structure, knowledge-strategy-impl

#### ✅ 11. pipeline-service
- **状态**: 已完成
- **描述**: 实现管道编排服务
- **完成内容**:
  - ✅ 实现ChainContext责任链执行上下文，包含执行状态管理和统计信息
  - ✅ 实现PipelineChain责任链处理框架，支持动态组装、执行和重试逻辑
  - ✅ 实现AgentPipelineService管道编排外观服务，提供统一的处理入口
  - ✅ 实现AgentProcessingResult结果封装类，包含完整的处理信息
  - ✅ 支持多种预定义处理链：标准链、快速链、知识增强链
  - ✅ 支持动态处理链创建和上下文感知的步骤选择
  - ✅ 集成错误处理、重试机制和性能监控
  - ✅ 支持异步处理和执行统计信息收集
- **预计工期**: 2天
- **依赖**: pipeline-steps-impl

#### ✅ 12. streaming-support
- **状态**: 已完成
- **描述**: 实现双向流式处理支持
- **完成内容**:
  - ✅ 创建LlmStreamingClient流式客户端，支持WebClient响应式处理
  - ✅ 增强ModelInvocationStep支持流式和非流式模式自动选择
  - ✅ 完善StreamingService的SSE连接管理和事件推送
  - ✅ 实现StreamingProgressListener进度监听和事件转发
  - ✅ 完善ChatController流式接口，支持GET/POST双重接口
  - ✅ 集成进度监听器到处理链中，支持实时进度推送
  - ✅ 添加流式处理状态查询和连接管理接口
- **预计工期**: 3天  
- **依赖**: pipeline-service

#### ✅ 13. rest-controllers
- **状态**: 已完成
- **描述**: 实现REST API控制器
- **完成内容**:
  - ✅ 创建AgentController核心API控制器，提供问答和知识检索接口
  - ✅ 实现基础问答接口(/api/v1/agent/chat)和高级问答接口(/api/v1/agent/chat/advanced)
  - ✅ 实现三种知识检索接口：向量检索、数据库检索、API检索
  - ✅ 创建GlobalExceptionHandler全局异常处理器，统一错误响应格式
  - ✅ 实现系统状态接口、处理链信息接口和统计管理接口
  - ✅ 创建完整的请求和响应DTO类，支持复杂参数传递
  - ✅ 创建API测试页面(api-test.html)，方便接口测试和调试
- **预计工期**: 2天
- **依赖**: streaming-support

#### ✅ 14. llm-client
- **状态**: 已完成
- **描述**: 实现LLM客户端
- **完成内容**:
  - ✅ 创建LlmClientConfig配置类，配置WebClient和重试策略
  - ✅ 创建EnhancedLlmClient增强版LLM客户端，支持多种模型API
  - ✅ 集成超时和重试机制，提高调用稳定性
  - ✅ 支持OpenAI、Claude、本地模型等多种LLM提供商
  - ✅ 实现流式响应处理和批量调用功能
  - ✅ 更新LlmStreamingClient集成增强版客户端
  - ✅ 完善application.yml中的LLM配置项，支持环境变量
- **预计工期**: 2天
- **依赖**: streaming-support

#### ✅ 15. context-builder
- **状态**: 已完成
- **描述**: 实现AgentContext建造者
- **完成内容**:
  - ✅ 实现Builder模式，提供流式API和上下文验证
  - ✅ 支持状态传递机制和资源清理管理
  - ✅ 添加defensive copying和便利方法
- **预计工期**: 1.5天
- **依赖**: agent-core-module

#### ✅ 16. configuration-properties
- **状态**: 已完成
- **描述**: 外部化配置管理
- **完成内容**:
  - ✅ 创建@ConfigurationProperties主配置类AgentProperties
  - ✅ 实现LlmProperties：LLM客户端配置（连接、重试、API、流式、默认模型）
  - ✅ 实现KnowledgeProperties：知识源配置（向量存储、SQL数据库、API源）
  - ✅ 实现PipelineProperties：管道配置（异步、超时、线程池、重试、监控）
  - ✅ 实现McpProperties：MCP协议配置（WebSocket、JSON-RPC、工具、资源）
  - ✅ 创建配置验证服务ConfigurationValidationService，支持业务规则验证
  - ✅ 创建敏感信息处理器SensitiveConfigurationHandler，支持掩码和加密
  - ✅ 集成Bean Validation和自定义验证逻辑
- **预计工期**: 1天
- **依赖**: agent-app-config

### 阶段3：扩展功能 (17-23)

#### ⏳ 17. mcp-server-impl
- **状态**: 待开始
- **描述**: 实现MCP服务器
- **任务内容**:
  - JSON-RPC 2.0协议实现
  - WebSocket连接管理
  - 工具注册和发现
  - 资源提供接口
- **预计工期**: 3天
- **依赖**: agent-mcp-structure

#### ⏳ 18. mcp-tools
- **状态**: 待开始
- **描述**: 实现MCP工具集
- **任务内容**:
  - knowledge_retriever工具
  - JSON Schema定义
  - 工具执行器
  - 安全权限控制
- **预计工期**: 2天
- **依赖**: mcp-server-impl, knowledge-strategy-impl

#### ⏳ 19. spring-ai-integration
- **状态**: 待开始
- **描述**: 集成Spring AI框架
- **任务内容**:
  - AI客户端配置
  - 向量存储集成
  - 嵌入模型配置
  - 对话模型集成
- **预计工期**: 2天
- **依赖**: llm-client

#### ⏳ 20. h2-database-setup
- **状态**: 待开始
- **描述**: 配置H2数据库
- **任务内容**:
  - 数据源配置
  - JPA实体定义
  - 初始化脚本
  - 控制台访问配置
- **预计工期**: 1天
- **依赖**: configuration-properties

#### ⏳ 21. global-exception-handler
- **状态**: 待开始
- **描述**: 全局异常处理
- **任务内容**:
  - @RestControllerAdvice实现
  - 异常映射和转换
  - 错误响应格式化
  - 日志记录策略
- **预计工期**: 1.5天
- **依赖**: rest-controllers

#### ⏳ 22. logging-tracing
- **状态**: 待开始
- **描述**: 日志和链路追踪
- **任务内容**:
  - MDC请求ID注入
  - 结构化日志格式
  - 链路追踪集成
  - 日志轮转配置
- **预计工期**: 1.5天
- **依赖**: global-exception-handler

#### ⏳ 23. monitoring-metrics
- **状态**: 待开始
- **描述**: 监控指标集成
- **任务内容**:
  - Micrometer指标配置
  - 自定义业务指标
  - Prometheus导出
  - 健康检查端点
- **预计工期**: 1.5天
- **依赖**: logging-tracing

### 阶段4：生产就绪 (24-31)

#### ⏳ 24. security-config
- **状态**: 待开始
- **描述**: 安全配置
- **任务内容**:
  - Spring Security配置
  - API认证授权
  - CORS跨域配置
  - 输入验证和过滤
- **预计工期**: 2天
- **依赖**: rest-controllers

#### ⏳ 25. integration-tests
- **状态**: 待开始
- **描述**: 集成测试
- **任务内容**:
  - TestContainers配置
  - API端到端测试
  - 流式处理测试
  - MCP协议测试
- **预计工期**: 3天
- **依赖**: 所有核心功能模块

#### ⏳ 26. unit-tests
- **状态**: 待开始
- **描述**: 单元测试
- **任务内容**:
  - 服务层单元测试
  - 控制器层测试
  - 策略模式测试
  - Mock和Stub配置
- **预计工期**: 2天
- **依赖**: integration-tests

#### ⏳ 27. fix-jacoco-java21
- **状态**: 待开始
- **描述**: 修复JaCoCo与Java 21兼容性
- **任务内容**:
  - 升级JaCoCo到0.8.11+版本
  - 配置Java 21支持
  - 测试覆盖率报告
  - CI/CD集成
- **预计工期**: 0.5天
- **依赖**: unit-tests

#### ⏳ 28. docker-deployment
- **状态**: 待开始
- **描述**: Docker部署配置
- **任务内容**:
  - Dockerfile优化
  - Jib插件配置
  - 多阶段构建
  - 镜像安全扫描
- **预计工期**: 1.5天
- **依赖**: fix-jacoco-java21

#### ⏳ 29. kubernetes-manifests
- **状态**: 待开始
- **描述**: Kubernetes部署清单
- **任务内容**:
  - Deployment配置
  - Service和Ingress
  - ConfigMap和Secret
  - 健康检查配置
- **预计工期**: 1.5天  
- **依赖**: docker-deployment

#### ⏳ 30. performance-optimization
- **状态**: 待开始
- **描述**: 性能优化
- **任务内容**:
  - 响应时间优化
  - 内存使用优化
  - 并发处理优化
  - 缓存策略实现
- **预计工期**: 2天
- **依赖**: kubernetes-manifests

#### ⏳ 31. documentation-completion
- **状态**: 待开始
- **描述**: 文档完善
- **任务内容**:
  - API文档补充
  - 部署指南更新
  - 开发指南完善
  - 故障排除手册
- **预计工期**: 1天
- **依赖**: performance-optimization

---

## 📊 里程碑计划

### 🎯 里程碑1：基础架构完成 (第1-2周) ✅ 已完成
- **目标**: 完成多模块架构搭建和基础配置
- **包含任务**: 1-7
- **交付物**: 可编译运行的多模块项目骨架

### 🎯 里程碑2：核心功能完成 (第3-5周) ✅ 已完成
- **目标**: 完成知识检索和处理流程核心功能
- **包含任务**: 8-16
- **交付物**: 基本的问答和知识检索功能

### 🎯 里程碑3：扩展功能完成 (第6-7周) ⏳ 进行中
- **目标**: 完成MCP协议支持和监控集成
- **包含任务**: 17-23  
- **交付物**: 完整功能的Agent系统

### 🎯 里程碑4：生产就绪 (第8-9周)
- **目标**: 完成测试、安全、部署等生产就绪配置
- **包含任务**: 24-31
- **交付物**: 可生产部署的完整系统

---

## 🔄 下一步行动

### 当前优先级
1. **mcp-server-impl** - 实现MCP服务器核心功能
2. **mcp-tools** - 实现MCP工具集
3. **spring-ai-integration** - 集成Spring AI框架

### 风险识别
- ⚠️ **JaCoCo兼容性**: 需要升级版本支持Java 21
- ⚠️ **流式处理复杂性**: 双向流式处理实现复杂
- ⚠️ **MCP协议文档**: 协议实现需要深入研究

### 资源需求
- **开发人员**: 1-2名Java开发人员
- **总工期**: 8-9周
- **技术栈**: Spring Boot 3.x, Java 21, Maven, Docker

---

## 📝 更新日志

**2025-07-04**
- 完成项目重构为多模块架构
- 完成Maven父级pom配置和依赖管理
- 修复依赖问题，成功安装所有模块
- 创建详细的TODO任务清单文档
- 完成任务10：pipeline-steps-impl（实现责任链处理步骤）
  - 完善PipelineStep接口和AbstractPipelineStep抽象类
  - 实现四个核心步骤：PlanningStep、KnowledgeRecallStep、PromptConstructionStep、ModelInvocationStep
  - 完善相关DTO和接口
- 完成任务11：pipeline-service（实现管道编排服务）
  - 实现ChainContext、PipelineChain、AgentPipelineService核心类
  - 支持责任链动态组装、错误处理和重试机制
  - 集成性能监控和执行统计功能
  - 提供多种预定义处理链和异步处理能力

**2025-07-05**
- 完成任务12：streaming-support（实现双向流式处理支持）
  - 创建LlmStreamingClient流式客户端，支持WebClient响应式处理
  - 增强ModelInvocationStep支持流式和非流式模式自动选择
  - 完善StreamingService的SSE连接管理和事件推送
  - 集成进度监听器到处理链中，支持实时进度推送
- 完成任务13：rest-controllers（实现REST API控制器）
  - 创建AgentController核心API控制器，提供问答和知识检索接口
  - 实现基础和高级问答接口，支持复杂参数传递
  - 创建GlobalExceptionHandler全局异常处理器，统一错误响应格式
  - 创建API测试页面(api-test.html)，方便接口测试和调试
- 完成任务14：llm-client（实现LLM客户端）
  - 创建LlmClientConfig配置类，配置WebClient和重试策略
  - 创建EnhancedLlmClient增强版LLM客户端，支持多种模型API
  - 集成超时和重试机制，支持OpenAI、Claude、本地模型等多种提供商
  - 完善application.yml中的LLM配置项，支持环境变量
- 完成任务15：context-builder（实现AgentContext建造者）
  - 实现Builder模式，提供流式API和上下文验证
  - 支持状态传递机制和资源清理管理
  - 添加defensive copying和便利方法
- 完成任务16：configuration-properties（外部化配置管理）
  - 创建完整的@ConfigurationProperties配置体系
  - 实现配置验证服务和敏感信息处理器
  - 支持多环境配置和业务规则验证

---

*最后更新: 2025-07-05 02:00* 