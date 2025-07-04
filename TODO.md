# Spring Boot Knowledge Agent - TODO 任务清单

## 📋 项目概述
基于设计文档（ragAgentPrd.md）实现Spring Boot知识Agent，采用领域驱动的多模块架构，支持责任链处理、多源知识检索、双向流式处理和MCP协议。

## 🎯 任务进度统计
- 总任务数：31
- 已完成：2/31 (6.5%)
- 进行中：0/31 (0%)
- 待开始：29/31 (93.5%)

---

## 📝 任务详细列表

### 阶段1：基础架构搭建 (1-7)

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

#### ⏳ 3. agent-core-module
- **状态**: 待开始
- **描述**: 实现核心基础模块
- **任务内容**:
  - 创建DTO类：AgentContext、KnowledgeChunk、PlanDetail等
  - 定义核心接口：PipelineStep、KnowledgeSourceStrategy
  - 实现自定义异常类
  - 创建工具类和常量定义
- **预计工期**: 2天
- **依赖**: project-restructure

#### ⏳ 4. agent-knowledge-structure
- **状态**: 待开始  
- **描述**: 搭建知识管理模块基础结构
- **任务内容**:
  - 创建策略接口实现
  - 设计知识源工厂类
  - 定义数据访问仓库接口
  - 配置数据源连接
- **预计工期**: 2天
- **依赖**: agent-core-module

#### ⏳ 5. agent-pipeline-structure
- **状态**: 待开始
- **描述**: 搭建处理流程模块基础结构  
- **任务内容**:
  - 创建责任链基础框架
  - 定义流程步骤模板
  - 实现外观服务类
  - 配置Spring组件扫描
- **预计工期**: 2天
- **依赖**: agent-core-module

#### ⏳ 6. agent-mcp-structure
- **状态**: 待开始
- **描述**: 搭建MCP协议模块基础结构
- **任务内容**:
  - 定义MCP工具接口
  - 创建协议处理器
  - 实现WebSocket端点
  - 配置JSON-RPC处理
- **预计工期**: 1.5天  
- **依赖**: agent-core-module

#### ⏳ 7. agent-app-config
- **状态**: 待开始
- **描述**: 配置应用模块和启动类
- **任务内容**:
  - 完善AgentApplication启动类
  - 配置组件扫描范围
  - 设置应用属性文件
  - 配置日志和监控
- **预计工期**: 1天
- **依赖**: 模块3-6全部完成

### 阶段2：核心功能模块 (8-16)

#### ⏳ 8. knowledge-repository
- **状态**: 待开始
- **描述**: 实现知识检索仓库层
- **任务内容**:
  - 实现向量数据库访问
  - 实现SQL数据库查询
  - 实现外部API调用
  - 配置连接池和事务
- **预计工期**: 3天
- **依赖**: agent-knowledge-structure

#### ⏳ 9. knowledge-strategy-impl
- **状态**: 待开始
- **描述**: 实现具体的知识检索策略
- **任务内容**:
  - VectorStoreStrategy实现
  - SqlDatabaseStrategy实现  
  - ApiSourceStrategy实现
  - 策略工厂完善
- **预计工期**: 3天
- **依赖**: knowledge-repository

#### ⏳ 10. pipeline-steps-impl
- **状态**: 待开始
- **描述**: 实现责任链处理步骤
- **任务内容**:
  - PlanningStep：查询规划
  - KnowledgeRecallStep：知识召回
  - PromptConstructionStep：prompt构建
  - ModelInvocationStep：模型调用
- **预计工期**: 4天
- **依赖**: agent-pipeline-structure, knowledge-strategy-impl

#### ⏳ 11. pipeline-service
- **状态**: 待开始
- **描述**: 实现管道编排服务
- **任务内容**:
  - AgentPipelineService实现
  - 责任链动态组装
  - 错误处理和重试
  - 性能监控集成
- **预计工期**: 2天
- **依赖**: pipeline-steps-impl

#### ⏳ 12. streaming-support
- **状态**: 待开始
- **描述**: 实现双向流式处理支持
- **任务内容**:
  - SseEmitter进度推送
  - WebClient响应式消费
  - 异步桥接处理
  - 流式响应转发
- **预计工期**: 3天  
- **依赖**: pipeline-service

#### ⏳ 13. rest-controllers
- **状态**: 待开始
- **描述**: 实现REST API控制器
- **任务内容**:
  - 问答接口实现
  - 流式接口实现
  - 知识检索接口
  - 错误处理器
- **预计工期**: 2天
- **依赖**: streaming-support

#### ⏳ 14. llm-client
- **状态**: 待开始
- **描述**: 实现LLM客户端
- **任务内容**:
  - WebClient配置
  - 模型API集成
  - 流式响应处理
  - 超时和重试机制
- **预计工期**: 2天
- **依赖**: streaming-support

#### ⏳ 15. context-builder
- **状态**: 待开始
- **描述**: 实现AgentContext建造者
- **任务内容**:
  - Builder模式实现
  - 上下文验证
  - 状态传递机制
  - 清理资源管理
- **预计工期**: 1.5天
- **依赖**: agent-core-module

#### ⏳ 16. configuration-properties
- **状态**: 待开始
- **描述**: 外部化配置管理
- **任务内容**:
  - @ConfigurationProperties类
  - 多环境配置文件
  - 敏感信息加密
  - 配置验证
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

### 🎯 里程碑1：基础架构完成 (第1-2周)
- **目标**: 完成多模块架构搭建和基础配置
- **包含任务**: 1-7
- **交付物**: 可编译运行的多模块项目骨架

### 🎯 里程碑2：核心功能完成 (第3-5周)  
- **目标**: 完成知识检索和处理流程核心功能
- **包含任务**: 8-16
- **交付物**: 基本的问答和知识检索功能

### 🎯 里程碑3：扩展功能完成 (第6-7周)
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
1. **agent-core-module** - 创建核心DTO和接口
2. **agent-knowledge-structure** - 搭建知识管理模块
3. **agent-pipeline-structure** - 搭建处理流程模块

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

---

*最后更新: 2025-07-04 23:44* 