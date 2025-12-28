# Spring AI vs Spring AI Alibaba 技术选型对比（PPT 专用）

本文档用于技术选型 PPT，提供 **Spring AI** 和 **Spring AI Alibaba** 在 Text-to-SQL 场景下的全面对比。

## 快速验证（按顺序执行）

### 1. 设置环境变量

```bash
# Windows (PowerShell)
$env:QWEN_API_KEY="your-dashscope-api-key"

# 或 CMD
set QWEN_API_KEY=your-dashscope-api-key
```

### 2. 启动 MCP Server（端口 8083）

```bash
cd mcp-server
mvn spring-boot:run
```

验证：http://localhost:8083/mcp/tools

### 3. 启动 Demo SAI（Spring AI 官方版本，端口 8081）

```bash
cd demo-sai
mvn spring-boot:run
```

### 4. 启动 Demo SAIA（Spring AI Alibaba 版本，端口 8082）

```bash
cd demo-saia
mvn spring-boot:run
```

## PPT 对比表格（复制即用）

| **对比维度**           | **Spring AI（官方）**                                | **Spring AI Alibaba**                                |
| ---------------------- | ---------------------------------------------------- | ---------------------------------------------------- |
| **演示地址**           | http://localhost:8081/                               | http://localhost:8082/                               |
| **版本**               | 1.0.0-M5（Milestone）                                | 1.0.0.2（GA 稳定版）                                 |
| **依赖引入**           | `spring-ai-openai-spring-boot-starter`               | `spring-ai-alibaba-starter-dashscope` + BOM          |
| **模型接入方式**       | OpenAI 兼容 API（通过 DashScope 兼容模式）           | 原生 DashScope SDK                                   |
| **配置 Key**           | `spring.ai.openai.*`                                 | `spring.ai.dashscope.*`                              |
| **API Key 配置**       | `QWEN_API_KEY`（环境变量）                           | `QWEN_API_KEY`（环境变量）                           |
| **默认模型**           | qwen-turbo                                           | qwen-plus                                            |
| **API 差异**           | `AssistantMessage.getContent()`                      | `AssistantMessage.getText()`                         |
| **Function Calling**   | 标准 Spring AI `@Bean` + `@Description`              | 同 Spring AI（完全兼容）                             |
| **ChatClient API**     | `ChatClient.create(chatModel).prompt()...`           | 同 Spring AI（API 一致）                             |
| **MCP Server 调用**    | WebClient（业务代码完全相同）                        | WebClient（业务代码完全相同）                        |
| **与阿里云生态集成度** | 低（需手动配置 base-url 为兼容模式）                | 高（原生支持，可直接用阿里云特性）                   |
| **稳定性**             | Milestone 版本，API 可能变更                         | GA 版本，生产可用                                    |
| **社区活跃度**         | Spring 官方社区                                      | 阿里云社区 + Spring AI 社区                          |
| **文档完整度**         | Spring AI 官方文档                                   | Spring AI Alibaba 官方文档 + 阿里云 DashScope 文档   |
| **适用场景**           | 多模型切换（OpenAI/Azure/等）、标准化架构            | 深度依赖阿里云生态、需要阿里云特性（如百炼大模型等） |

## 核心代码对比

### 依赖配置

#### Spring AI（demo-sai）

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.0.0-M5</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
```

#### Spring AI Alibaba（demo-saia）

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.alibaba.cloud.ai</groupId>
            <artifactId>spring-ai-alibaba-bom</artifactId>
            <version>1.0.0.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
</dependency>
```

### application.yml 配置

#### Spring AI（demo-sai）

```yaml
spring:
  ai:
    openai:
      base-url: https://dashscope.aliyuncs.com/compatible-mode
      api-key: ${QWEN_API_KEY}
      chat:
        options:
          model: qwen-turbo
          temperature: 0.0
```

#### Spring AI Alibaba（demo-saia）

```yaml
spring:
  ai:
    dashscope:
      api-key: ${QWEN_API_KEY}
      chat:
        options:
          model: qwen-plus
          temperature: 0.0
```

### Controller 代码差异

#### Spring AI（demo-sai）

```java
String response = chatResponse.getResult().getOutput().getContent();
```

#### Spring AI Alibaba（demo-saia）

```java
String response = chatResponse.getResult().getOutput().getText();
```

**其他业务代码（Controller、Service、Function Config）完全一致。**

## 技术选型建议

### 选择 Spring AI（官方）的场景

1. **多云/多模型切换**：需要在 OpenAI、Azure OpenAI、Anthropic 等多个模型间切换
2. **标准化优先**：希望跟随 Spring 官方生态，保持技术栈标准化
3. **国际化团队**：团队主要使用 Spring 官方文档和社区资源
4. **未来灵活性**：未确定长期使用哪家云厂商

### 选择 Spring AI Alibaba 的场景

1. **深度依赖阿里云**：已使用阿里云生态（Nacos、ARMS、百炼大模型等）
2. **生产稳定性**：需要稳定 GA 版本，不能接受 Milestone 版本的潜在变更
3. **阿里云特性**：需要使用阿里云独有功能（如向量数据库 Tablestore、Prompt 管理等）
4. **中文支持**：团队更依赖中文文档和阿里云社区

## 实测差异（仅供参考）

| **测试项**         | **Spring AI** | **Spring AI Alibaba** |
| ------------------ | ------------- | --------------------- |
| **构建时间**       | ~3.3s         | ~1.8s                 |
| **启动时间**       | 约 5s         | 约 5s                 |
| **API 响应速度**   | 基本一致      | 基本一致              |
| **Token 使用统计** | 完整          | 完整                  |
| **错误日志清晰度** | 清晰          | 清晰                  |

## 迁移成本评估

### 从 Spring AI 迁移到 Spring AI Alibaba

**工作量：低**

需要修改：
1. `pom.xml` 依赖（BOM + starter）
2. `application.yml` 配置 key 路径
3. Controller 中 `getContent()` → `getText()`

**不需要修改**：
- Function Config（`@Bean` + `@Description`）
- Service 层业务逻辑
- DTO、Controller 结构
- Web UI 前端代码

### 从 Spring AI Alibaba 迁移到 Spring AI

**工作量：低**

反向操作即可，改动点相同。

## 版本信息验证

### Spring AI（demo-sai）

- **BOM**: `org.springframework.ai:spring-ai-bom:1.0.0-M5`
- **Starter**: `org.springframework.ai:spring-ai-openai-spring-boot-starter`
- **Maven Central**: ✅ 可用
- **来源**: https://repo.maven.apache.org/maven2/org/springframework/ai/

### Spring AI Alibaba（demo-saia）

- **BOM**: `com.alibaba.cloud.ai:spring-ai-alibaba-bom:1.0.0.2`
- **Starter**: `com.alibaba.cloud.ai:spring-ai-alibaba-starter-dashscope`
- **Maven Central**: ✅ 可用
- **来源**: https://central.sonatype.com/artifact/com.alibaba.cloud.ai/spring-ai-alibaba-bom/1.0.0.2
- **GitHub Release**: https://github.com/alibaba/spring-ai-alibaba/releases/tag/v1.0.0.2

## 演示测试用例

在两个 Web UI 中分别输入以下问题，验证结果一致性：

1. `查询所有客户的姓名和邮箱`
2. `统计每个客户的订单总数`
3. `查询总金额最高的订单`
4. `列出所有待发货的订单`

**预期**：两个版本生成的 SQL、查询结果和中文解释应基本一致。

## 结论

- **技术实现**：两者在相同业务场景下，代码几乎完全一致（仅 API 名称微调）
- **功能完整性**：均完整支持 Function Calling、Tool Calling、MCP Server 集成
- **选型依据**：主要取决于团队生态绑定（阿里云 vs 多云）和版本稳定性要求（GA vs Milestone）
- **迁移难度**：极低，核心业务代码无需改动

---

**生成时间**：2025-12-23
**验证环境**：Windows、Java 17、Maven 3.x
