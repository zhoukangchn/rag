

# **设计与架构报告：Spring Boot 知识 Agent**

## **I. 基础架构：领域驱动的多模块单体**

为了确保可扩展性、可维护性和明确的关注点分离，知识 Agent 将采用 **领域驱动的多模块单体（Domain-Driven Multi-module Monolith）** 架构，并在 Spring Boot 生态系统中实现。此方法将应用按照业务能力而非技术分层进行组织，划分为不同的模块 。

这是一个经过深思熟虑的选择，优于技术分层结构（例如 agent-controllers、agent-services）。技术分层常常导致高耦合、低内聚，并最终形成一个“大泥球”式的架构，使得简单的变更也需要跨多个模块进行修改 。相比之下，领域驱动的结构确保了大部分变更都限制在单个模块内，从而增强了可维护性 1。

### **建议的模块结构**

项目将组织为一个父项目，包含以下子模块，由 Maven 或 Gradle 管理 ：

* agent-app：**可执行应用模块**。它聚合所有其他模块，并包含 main 方法和主配置文件。此模块将生成最终的可执行 JAR 包 1。  
* agent-core：**核心基础模块**。包含共享组件，如数据传输对象（DTO）、自定义异常、工具类以及基础接口（PipelineStep, KnowledgeSourceStrategy）3。  
* agent-pipeline：**核心流程模块**。负责编排整个 Agent 的工作流，从接收用户查询到返回最终响应。它包含 REST 控制器、责任链实现以及流式处理逻辑 4。  
* agent-knowledge：**知识管理模块**。此模块封装了所有与知识召回相关的逻辑。它将包含用于访问各种数据源（向量数据库、SQL 数据库、API）的策略和相应的仓库接口 5。  
* agent-mcp：**模型-上下文协议（MCP）模块**。一个专用于实现模型-上下文协议（MCP）服务端的模块，将 Agent 的能力暴露给更广泛的 AI 生态系统 6。

### **模块间通信**

为维持严格的边界和低耦合，模块间将仅通过定义在 agent-core 模块中的公共 API（接口）和 DTO 进行通信。禁止直接访问其他模块的内部实现或实体类 3。应优先使用 Gradle 的

implementation 作用域而非 api，以防止传递性依赖泄漏 。

**表1：模块职责**

| 模块名称 | 主要职责 | 关键依赖 |
| :---- | :---- | :---- |
| agent-app | 组装并构建可执行应用。 | 所有其他 agent-\* 模块 |
| agent-core | 提供共享的 DTO、异常和核心接口。 | 无 |
| agent-pipeline | 编排 Agent 的四步流程并管理流式处理。 | agent-core, agent-knowledge, agent-mcp |
| agent-knowledge | 实现从各种数据源进行知识召回。 | agent-core |
| agent-mcp | 通过 MCP 标准暴露 Agent 的能力。 | agent-core, agent-knowledge |

---

## **II. 核心逻辑：责任链模式**

Agent 的顺序工作流（“搜索规划 \-\> 知识召回 \-\> prompt 拼接 \-\> 调用模型”）非常适合采用 **责任链（Chain of Responsibility）** 设计模式。该模式创建了一个处理对象（处理器）链，允许请求沿着链传递，直到被处理 4。

### **AgentContext 与建造者模式**

一个中心的 AgentContext 对象将在处理链中传递状态。为了管理其复杂构造，将使用 **建造者（Builder）** 模式。这为分步创建对象提供了一个流畅、可读的 API，并允许强制执行必需参数（如初始用户查询）14。

### **PipelineStep 接口**

链中的所有处理器都将实现一个通用接口。

Java

// 位于 agent-core 模块  
public interface PipelineStep {  
    void process(AgentContext context);  
    void setNext(PipelineStep nextStep);  
}

### **使用 Spring 组装责任链**

我们将利用 Spring 的特性进行优雅、自动化的组装，而非手动链接处理器。

1. **@Order 注解**：每个 PipelineStep 实现（例如 PlanningStep、KnowledgeRecallStep）都将是一个 Spring @Service，并使用 @Order 注解来定义其在链中的位置 15。  
2. **外观（Facade）与依赖注入（DI）**：一个 AgentPipelineService 将作为外观，简化客户端与链的交互。其构造函数将注入一个 List\<PipelineStep\>。Spring 会根据 @Order 的值自动将所有 PipelineStep Bean 按顺序填充到此列表中 15。随后，该外观会以编程方式将这些步骤链接在一起。

Java

// 位于 agent-pipeline 模块  
@Service  
public class AgentPipelineService {  
    private final PipelineStep firstStep;

    @Autowired  
    public AgentPipelineService(List\<PipelineStep\> steps) {  
        // Spring 按正确顺序注入步骤  
        this.firstStep \= steps.get(0);  
        for (int i \= 0; i \< steps.size() \- 1; i++) {  
            steps.get(i).setNext(steps.get(i \+ 1));  
        }  
    }

    public void execute(AgentContext context) {  
        firstStep.process(context);  
    }  
}

这种方法使系统具有高度的可扩展性。向流程中添加新步骤只需创建一个带有 @Service 和 @Order 注解的新类，而无需修改 AgentPipelineService。

---

## **III. 知识召回：策略模式**

为了从各种数据源（向量存储、SQL 数据库等）动态检索信息，KnowledgeRecallStep 将使用 **策略（Strategy）** 模式。该模式定义了一系列算法，将每个算法封装起来，并使它们在运行时可以互换 5。

### **KnowledgeSourceStrategy 接口**

每种数据源访问方法都将是一个“策略”，实现一个通用接口。

Java

// 位于 agent-knowledge 模块  
public interface KnowledgeSourceStrategy {  
    List\<KnowledgeChunk\> retrieve(PlanDetail planDetail);  
    String getStrategyName();  
}

### **策略工厂：Map 注入**

为了在运行时根据执行计划选择正确的策略，需要一个工厂。我们将使用 Spring 的依赖注入功能来填充一个策略的 Map，而不是使用脆弱的 if-else 块 5。

1. **命名 Bean**：每个策略实现都将是一个带有唯一名称的 Spring @Service（例如 @Service("vector\_store")）。  
2. **Map 注入**：KnowledgeSourceFactory 将注入一个 Map\<String, KnowledgeSourceStrategy\>。Spring 会自动填充此 Map，使用 Bean 的名称作为键。

Java

// 位于 agent-knowledge 模块  
@Service  
public class KnowledgeSourceFactory {  
    private final Map\<String, KnowledgeSourceStrategy\> strategies;

    @Autowired  
    public KnowledgeSourceFactory(Map\<String, KnowledgeSourceStrategy\> strategies) {  
        this.strategies \= strategies;  
    }

    public KnowledgeSourceStrategy getStrategy(String name) {  
        // O(1) 查找  
        return strategies.get(name);  
    }  
}

这个设计是开闭原则的典范。要支持新的数据源，开发者只需添加一个新的、实现了策略接口的命名 @Service。工厂无需任何修改。

---

## **IV. 实时交互：双向流式架构**

此架构支持两个并行的流式处理需求：向客户端发送处理进度更新，以及中继来自外部 LLM 的流式响应。

### **A. 服务端 \-\> 客户端：使用 SseEmitter 推送进度更新**

对于从服务器发送实时进度更新，我们将在标准的基于 Servlet 的控制器中使用 Spring MVC 的 SseEmitter 16。

1. **控制器端点**：一个 produces \= MediaType.TEXT\_EVENT\_STREAM\_VALUE 的控制器方法将创建并返回一个 SseEmitter 实例，从而建立一个持久的 HTTP 连接 16。  
2. **上下文传递**：将 SseEmitter 放入 AgentContext 中，使其在整个处理流程中可用。  
3. **发送事件**：每个 PipelineStep 将使用 emitter.send() 方法在完成其任务后向客户端推送状态更新。  
4. **生命周期管理**：在 SseEmitter 上注册 onCompletion、onTimeout 和 onError 回调，以处理连接终止并确保资源得到正确清理 16。

### **B. 外部 API \-\> 客户端：桥接响应式与 Servlet 栈**

为了消费来自 LLM 的流式响应（通常是响应式的）并通过我们的 Servlet 应用进行转发，我们将创建一个“异步桥梁”。

1. **依赖**：添加 spring-boot-starter-webflux 依赖。这使得在传统的 Spring MVC 应用中可以使用响应式的 WebClient，而无需转换整个技术栈 。  
2. **使用 WebClient 消费**：一个 LlmClient 服务将使用 WebClient 调用 LLM 的流式端点。此调用将返回一个 Flux\<DataBuffer\>，代表数据块流 。  
3. **桥梁**：ModelInvocationStep 将执行桥接逻辑：  
   * 它将从 LlmClient 获取 Flux\<DataBuffer\>，并从 AgentContext 获取 SseEmitter。  
   * 它将一个异步任务提交到一个专用的 ExecutorService。  
   * 在此任务内部，关键操作是 DataBufferUtils.write(flux, outputStream)，它将响应式的 Flux 直接输送到 SseEmitter 底层的 OutputStream 17。  
   * **至关重要的是**，这是一个阻塞操作，必须在后台线程上运行，以避免耗尽主服务器线程池。  
   * 消费后的 DataBuffer 必须通过 .doOnNext(DataBufferUtils::release) 释放以防止内存泄漏 17。  
   * SseEmitter 的生命周期（complete() 或 completeWithError()）与 Flux 的完成或错误信号绑定。

---

## **V. 互操作性：模型-上下文协议（MCP）服务端**

为了使 Agent 成为 AI 生态系统中可重用的组件，我们将实现 **模型-上下文协议（Model-Context Protocol, MCP）**。MCP 是一个开放标准，旨在标准化应用如何连接到外部工具和数据源，类似于 AI 的 USB-C 接口 。

### **MCP 概念**

* **协议**：基于 JSON-RPC 2.0，通常通过 WebSocket 运行 。  
* **角色**：一个 **Host**（例如 AI 聊天应用）连接到一个 **Server**（我们的 Agent）。  
* **原语**：Server 将其能力暴露为：  
  * **工具（Tools）**：Host 可以请求 Server 执行的功能（例如，运行一次搜索）。  
  * **资源（Resources）**：Server 可以提供的上下文数据（例如，可用数据库列表）。  
  * **提示（Prompts）**：模板化的工作流 。

### **在 agent-mcp 中实现**

agent-mcp 模块将实现一个 MCP Server，将 Agent 的核心功能作为 MCP 工具暴露出去。

* **knowledge\_retriever 工具**：知识召回能力将被暴露为一个 MCP 工具。  
  * **模式（Schema）**：工具的输入和输出参数将使用 **JSON Schema** 进行严格定义，这是协议的要求 。输入模式将匹配我们的 Plan DTO，输出将匹配 List\<KnowledgeChunk\>。  
  * **执行**：当服务器收到此工具的 tool/execute 请求时，它将调用 KnowledgeSourceFactory 来执行召回并返回结果。  
* **安全性**：实现将遵循 MCP 严格的安全原则，包括在执行任何工具前，通过健全的机制获取主机的身份验证、授权和用户同意 。

---

## **VI. 最终代码结构与生产就绪性**

### **项目文件结构**

agent-project/  
├── build.gradle  
├── settings.gradle  
│  
├── agent-app/  
│   └── src/main/java/com/example/agent/app/  
│       └── AgentApplication.java  
│  
├── agent-core/  
│   └── src/main/java/com/example/agent/core/  
│       ├── dto/  
│       │   ├── AgentContext.java  
│       │   └── KnowledgeChunk.java  
│       ├── exception/  
│       └── model/  
│           ├── PipelineStep.java  
│           └── KnowledgeSourceStrategy.java  
│  
├── agent-pipeline/  
│   └── src/main/java/com/example/agent/pipeline/  
│       ├── chain/  
│       │   ├── PlanningStep.java  
│       │   └── KnowledgeRecallStep.java  
│       ├── client/  
│       │   └── LlmClient.java  
│       ├── controller/  
│       │   └── AgentController.java  
│       └── service/  
│           └── AgentPipelineService.java  
│  
├── agent-knowledge/  
│   └── src/main/java/com/example/agent/knowledge/  
│       ├── strategy/  
│       │   ├── VectorStoreStrategy.java  
│       │   └── SqlDatabaseStrategy.java  
│       └── factory/  
│           └── KnowledgeSourceFactory.java  
│  
└── agent-mcp/  
    └── src/main/java/com/example/agent/mcp/  
        ├── endpoint/  
        │   └── McpEndpoint.java  
        └── provider/  
            └── McpToolProvider.java

### **生产考量**

* **配置**：使用 @ConfigurationProperties 将所有设置（API 密钥、URL 等）外部化。可以考虑使用 Springify Multiconfig 等库，以允许每个模块拥有自己的配置文件 。  
* **可测试性**：所选的模式（策略、责任链）和对依赖注入的依赖使得组件具有高度的单元可测试性。  
* **错误处理**：使用 @RestControllerAdvice 实现一个全局异常处理器，将自定义业务异常映射到适当的 HTTP 响应。  
* **日志与监控**：使用 MDC 注入唯一的请求 ID 以进行追踪。通过 Micrometer 暴露关键指标给 Prometheus 等监控工具。


