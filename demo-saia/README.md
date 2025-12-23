# Demo SAIA - Spring AI Alibaba 实现版本

基于 **Spring AI Alibaba 1.0.0.2（稳定 GA 版本）** 实现的 Text-to-SQL 智能代理。

## 技术栈

- **Spring Boot**: 3.5.9
- **Spring AI Alibaba**: 1.0.0.2（GA）
- **DashScope API**: 阿里云千问大模型
- **MCP Server**: 数据库工具接入

## 依赖版本信息

### 核心依赖（Maven Central 已验证）

```xml
<dependencyManagement>
    <dependencies>
        <!-- 重要：必须显式指定 Spring AI 1.0.0 GA 版本 -->
        <!-- 避免与父 POM 的 1.0.0-M5 冲突导致 NoSuchMethodError -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

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

**版本验证来源**：
- Spring AI Alibaba: https://github.com/alibaba/spring-ai-alibaba/releases/tag/v1.0.0.2
- Spring AI Alibaba BOM: https://central.sonatype.com/artifact/com.alibaba.cloud.ai/spring-ai-alibaba-bom/1.0.0.2
- Spring AI GA: https://repo.maven.apache.org/maven2/org/springframework/ai/spring-ai-bom/1.0.0/

## 启动前准备

### 1. 环境变量配置

必须设置 DashScope API Key：

```bash
# Windows (CMD)
set QWEN_API_KEY=your-dashscope-api-key

# Windows (PowerShell)
$env:QWEN_API_KEY="your-dashscope-api-key"

# Linux/Mac
export QWEN_API_KEY=your-dashscope-api-key
```

获取 API Key：https://dashscope.console.aliyun.com/apiKey

### 2. 启动 MCP Server

在根目录运行：

```bash
cd mcp-server
mvn spring-boot:run
```

验证：http://localhost:8083/mcp/tools

### 3. 启动 Demo SAIA

```bash
cd demo-saia
mvn spring-boot:run
```

## 验收测试

### 1. 健康检查

浏览器打开：http://localhost:8082/actuator/health

预期：`{"status":"UP"}`

### 2. Web UI 演示

浏览器打开：**http://localhost:8082/**

输入问题：`查询所有客户的姓名和邮箱`

**预期结果**：
- 生成 SQL：`SELECT name, email FROM customers`
- 查询结果：显示客户数据
- 中文解释：简短说明查询内容

### 3. API 调用

```bash
curl -X POST http://localhost:8082/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{"question": "查询所有客户的姓名和邮箱"}'
```

### 4. 日志验证

应能看到：
- `=== 调用 MCP Tool: schema.get ===`
- `=== 调用 MCP Tool: sql.run ===`
- `LLM 最终响应: ...`

## 配置说明

### application.yml

```yaml
spring:
  ai:
    dashscope:
      api-key: ${QWEN_API_KEY}  # 从环境变量读取
      chat:
        options:
          model: qwen-plus        # 使用 qwen-plus（稳定且功能强大）
          temperature: 0.0

mcp:
  server:
    base-url: http://127.0.0.1:8083  # MCP Server 地址

server:
  port: 8082  # 服务端口
```

### 模型选择

- **qwen-plus**（默认）：平衡性能和成本
- **qwen-turbo**：更快但功能较弱
- **qwen-max**：最强但成本高

## 与 Demo SAI 对比

| 项目               | Demo SAI (Spring AI)             | Demo SAIA (Spring AI Alibaba) |
| ------------------ | -------------------------------- | ----------------------------- |
| **端口**           | 8081                             | 8082                          |
| **框架版本**       | Spring AI 1.0.0-M5（Milestone）  | Spring AI 1.0.0（GA）         |
| **Alibaba 版本**   | -                                | Spring AI Alibaba 1.0.0.2     |
| **模型接入**       | OpenAI 兼容 API（DashScope）     | 原生 DashScope SDK            |
| **配置方式**       | `spring.ai.openai.*`             | `spring.ai.dashscope.*`       |
| **API Key**        | `QWEN_API_KEY`                   | `QWEN_API_KEY`                |
| **API 差异**       | `.functions(...)`                | `.toolNames(...)`             |
| **消息 API**       | `.getContent()`                  | `.getText()`                  |
| **Function Bean**  | 标准 Spring AI `@Bean`           | 同 Spring AI                  |

## 常见问题

### 1. 构建失败：找不到依赖

确保使用 BOM 方式引入依赖，版本号由 BOM 管理。

### 2. 启动失败：API Key 错误

检查环境变量 `QWEN_API_KEY` 是否正确设置。

### 3. 连接失败：MCP Server 未启动

确保 `mcp-server` 已在 8083 端口运行。

## 文档链接

- Spring AI Alibaba 官网：https://java2ai.com
- GitHub 仓库：https://github.com/alibaba/spring-ai-alibaba
- DashScope 文档：https://help.aliyun.com/zh/dashscope
