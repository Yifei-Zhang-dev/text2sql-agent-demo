# Spring AI Demo - Text-to-SQL Agent

一个完整的 Text-to-SQL 演示项目，对比 **Spring AI** 与 **Spring AI Alibaba** 两个框架在相同场景下的表现。项目实现了基于 LLM 的自然语言转 SQL 查询功能，通过 MCP (Model Context Protocol) 工具调用实现数据库交互，并提供可视化 Web 界面展示结构化查询结果。

---

## 📋 目录

- [项目概述](#-项目概述)
- [核心特性](#-核心特性)
- [技术架构](#-技术架构)
- [项目结构](#-项目结构)
- [快速开始](#-快速开始)
- [使用指南](#-使用指南)
- [API 文档](#-api-文档)
- [配置说明](#-配置说明)
- [功能演示](#-功能演示)
- [技术细节](#-技术细节)

---

## 🎯 项目概述

本项目是一个 **Text-to-SQL Agent** 的完整实现，旨在：

1. **对比评估** Spring AI 与 Spring AI Alibaba 两个框架的功能和性能
2. **演示 LLM Function Calling** 如何与外部工具（MCP Server）交互
3. **展示 Text-to-SQL 完整闭环**：自然语言 → 表结构获取 → SQL 生成 → 查询执行 → 结果展示
4. **提供生产级参考实现**：包含完整的错误处理、日志、Web UI 等

### 为什么选择这个项目？

- ✅ **真实可运行**：内置 H2 数据库和测试数据，开箱即用
- ✅ **结构化结果展示**：查询结果以表格形式展示，直观清晰
- ✅ **框架对比**：同一问题可分别调用两个框架，便于对比差异
- ✅ **标准 API**：基于 Spring AI 1.0.0 GA 标准 API，代码简洁统一
- ✅ **完整文档**：详细的配置说明、API 文档和使用指南

---

## ✨ 核心特性

### 1. 双框架实现

| 模块 | 框架 | LLM 提供商 | 端口 |
|------|------|-----------|------|
| **demo-sai** | Spring AI 1.0.0 | OpenAI (GPT-4/GPT-3.5) | 8081 |
| **demo-saia** | Spring AI Alibaba | 阿里云通义千问 (Qwen) | 8082 |

两个模块接口返回结构完全一致，便于对比测试。

### 2. 结构化查询结果展示

- **SQL 展示**：显示 LLM 生成并执行的 SQL 语句
- **表格结果**：动态生成 HTML 表格，包含表头和数据行
- **统计信息**：显示总行数和已展示行数（限制 20 行防止卡顿）
- **LLM 解释**：自然语言总结查询结果（1-2 句话）

### 3. MCP 工具调用

基于 [Model Context Protocol](https://modelcontextprotocol.io/) 标准实现工具调用：

- **schema.get**：获取数据库表结构（表名、字段、类型、注释）
- **sql.run**：执行 SELECT 查询，返回结构化数据（columns, rows, rowCount）

### 4. 可视化 Web UI

- 渐变紫色主题，现代化设计
- 示例问题快速填充
- 实时加载状态提示
- 错误友好展示
- 支持 Ctrl+Enter 快捷提交

---

## 🏗️ 技术架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         Web Browser                              │
│                    (http://localhost:8081/8082)                  │
└────────────────────────────────┬────────────────────────────────┘
                                 │ HTTP
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Spring Boot Application                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  AgentController (REST API)                              │   │
│  │    POST /agent/text2sql                                  │   │
│  └──────────────┬───────────────────────────────────────────┘   │
│                 │                                                │
│  ┌──────────────▼───────────────────────────────────────────┐   │
│  │  Text2SqlService                                         │   │
│  │  - 调用 ChatClient (Spring AI 1.0.0 API)                │   │
│  │  - Function Calling (schemaGet, sqlRun)                  │   │
│  │  - 组装响应 (SQL + QueryResult + Explanation)           │   │
│  └──────────────┬───────────────────────────────────────────┘   │
│                 │                                                │
│  ┌──────────────▼───────────────────────────────────────────┐   │
│  │  Spring AI Functions                                     │   │
│  │  - SchemaGetFunction.apply()                             │   │
│  │  - SqlRunFunction.apply()                                │   │
│  └──────────────┬───────────────────────────────────────────┘   │
│                 │                                                │
│  ┌──────────────▼───────────────────────────────────────────┐   │
│  │  McpToolService (WebClient)                              │   │
│  │  - getSchema(tableName)                                  │   │
│  │  - runSql(sql)                                           │   │
│  │  - saveQueryResult() → ThreadLocal                       │   │
│  └──────────────┬───────────────────────────────────────────┘   │
└─────────────────┼───────────────────────────────────────────────┘
                  │ HTTP
                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                        MCP Server (Port 9000)                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  McpToolController                                       │   │
│  │  - POST /mcp/tools/schema.get                            │   │
│  │  - POST /mcp/tools/sql.run                               │   │
│  └──────────────┬───────────────────────────────────────────┘   │
│                 │                                                │
│  ┌──────────────▼───────────────────────────────────────────┐   │
│  │  SqlService                                              │   │
│  │  - JDBC Template                                         │   │
│  │  - 查询表结构、执行 SQL                                  │   │
│  └──────────────┬───────────────────────────────────────────┘   │
│                 │                                                │
│  ┌──────────────▼───────────────────────────────────────────┐   │
│  │  H2 Database (In-Memory)                                 │   │
│  │  - customers (客户表)                                    │   │
│  │  - orders (订单表)                                       │   │
│  │  - order_items (订单明细表)                             │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                   External LLM Services                          │
│  - OpenAI API (GPT-4 / GPT-3.5)         [demo-sai]              │
│  - 阿里云通义千问 (Qwen)                [demo-saia]             │
└─────────────────────────────────────────────────────────────────┘
```

### 数据流向

```
用户输入（自然语言问题）
    ↓
Web UI → POST /agent/text2sql
    ↓
Text2SqlService.executeText2Sql()
    ├─→ ChatClient.prompt()
    │       ├─→ System Prompt（指导 LLM 如何调用工具）
    │       ├─→ User Question
    │       └─→ Tool Names（schemaGet, sqlRun）
    │
    ├─→ LLM 推理 → 决定调用 schemaGet
    │
    ├─→ SchemaGetFunction.apply("customers")
    │       └─→ McpToolService.getSchema()
    │               └─→ HTTP POST → MCP Server /mcp/tools/schema.get
    │                       └─→ 返回表结构 JSON
    │
    ├─→ LLM 推理 → 根据表结构生成 SQL
    │
    ├─→ SqlRunFunction.apply("SELECT NAME FROM customers")
    │       └─→ McpToolService.runSql()
    │               ├─→ HTTP POST → MCP Server /mcp/tools/sql.run
    │               │       └─→ 返回 {columns, rows, rowCount}
    │               └─→ saveQueryResult() → ThreadLocal<QueryResult>
    │
    ├─→ LLM 推理 → 生成自然语言解释
    │
    └─→ 组装响应：Text2SqlResponse
            ├─→ sql: "SELECT NAME FROM customers"
            ├─→ result: {columns: ["NAME"], rows: [["张三"], ...], rowCount: 10}
            └─→ explanation: "查询返回了 10 位客户的姓名。"
    ↓
Web UI 渲染
    ├─→ 【生成的 SQL】区域：显示 SQL 语句
    ├─→ 【查询结果】区域：动态生成 HTML 表格
    └─→ 【结果解释】区域：显示 LLM 自然语言总结
```

---

## 📁 项目结构

```
spring-ai-demo/
├── README.md                              # 项目总览文档
├── pom.xml                                # 父级 Maven 配置
├── .gitignore
│
├── mcp-server/                            # MCP Server 模块（端口 9000）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/mcp/
│       │   ├── McpServerApplication.java             # 启动类
│       │   ├── controller/
│       │   │   └── McpToolController.java            # MCP 工具接口
│       │   ├── service/
│       │   │   └── SqlService.java                   # SQL 执行逻辑
│       │   └── dto/
│       │       ├── SchemaRequest/Response.java
│       │       └── SqlRequest/Response.java
│       └── resources/
│           ├── application.yml                       # 端口 9000
│           ├── schema.sql                            # DDL
│           └── data.sql                              # 测试数据
│
├── demo-sai/                              # Spring AI 版本（端口 8081）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/demo/
│       │   ├── DemoSaiApplication.java               # 启动类
│       │   ├── config/
│       │   │   ├── ChatClientConfig.java             # ChatClient Bean
│       │   │   └── McpClientConfig.java              # WebClient Bean
│       │   ├── controller/
│       │   │   └── AgentController.java              # POST /agent/text2sql
│       │   ├── service/
│       │   │   ├── Text2SqlService.java              # 核心业务逻辑
│       │   │   └── McpToolService.java               # MCP 工具调用
│       │   ├── dto/
│       │   │   ├── Text2SqlRequest.java              # {question}
│       │   │   ├── Text2SqlResponse.java             # {sql, result, explanation}
│       │   │   └── QueryResult.java                  # {columns, rows, rowCount}
│       │   └── function/
│       │       ├── SchemaGetFunction.java            # Spring AI Function
│       │       └── SqlRunFunction.java               # Spring AI Function
│       └── resources/
│           ├── application.yml                       # OpenAI API 配置
│           └── static/
│               └── index.html                        # Web UI
│
└── demo-saia/                             # Spring AI Alibaba 版本（端口 8082）
    ├── pom.xml
    └── src/main/
        ├── java/com/example/demosaia/
        │   └── ... (与 demo-sai 结构完全相同)
        └── resources/
            ├── application.yml                       # 阿里云 API 配置
            └── static/
                └── index.html                        # Web UI
```

---

## 🚀 快速开始

### 前置要求

- **Java 17+**
- **Maven 3.6+**
- **OpenAI API Key** (demo-sai) 或 **阿里云 API Key** (demo-saia)

### 1. 克隆项目

```bash
git clone https://github.com/your-username/spring-ai-demo.git
cd spring-ai-demo
```

### 2. 配置 API Keys

#### demo-sai (OpenAI)

编辑 `demo-sai/src/main/resources/application.yml`：

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}  # 或直接填写 sk-xxxxx
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: gpt-4
          temperature: 0.0
```

**环境变量方式**（推荐）：

```bash
export OPENAI_API_KEY=sk-xxxxx
export OPENAI_BASE_URL=https://api.openai.com  # 可选，使用代理时修改
```

#### demo-saia (阿里云通义千问)

编辑 `demo-saia/src/main/resources/application.yml`：

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}  # 或直接填写
      chat:
        options:
          model: qwen-plus
          temperature: 0.0
```

**环境变量方式**（推荐）：

```bash
export DASHSCOPE_API_KEY=sk-xxxxx
```

### 3. 启动服务

在**三个独立终端**中分别运行：

```bash
# 终端 1: 启动 MCP Server
cd mcp-server
mvn spring-boot:run

# 终端 2: 启动 demo-sai (Spring AI)
cd demo-sai
mvn spring-boot:run

# 终端 3: 启动 demo-saia (Spring AI Alibaba)
cd demo-saia
mvn spring-boot:run
```

### 4. 访问 Web UI

- **demo-sai**: http://localhost:8081
- **demo-saia**: http://localhost:8082
- **MCP Server 健康检查**: http://localhost:9000/actuator/health

### 5. 测试示例

在 Web UI 中输入：

```
列出所有客户的姓名和邮箱
```

或点击页面上的示例按钮。

---

## 📖 使用指南

### Web UI 操作

1. **输入问题**：在文本框中输入自然语言问题
2. **示例按钮**：点击快速填充预设问题
3. **提交查询**：点击按钮或按 `Ctrl+Enter`
4. **查看结果**：
   - **生成的 SQL**：LLM 生成的 SQL 语句
   - **查询结果**：表格展示数据（前 20 行）
   - **结果解释**：LLM 的自然语言总结

### 示例问题

| 问题 | 说明 |
|------|------|
| 列出所有客户的姓名和邮箱 | 简单查询 |
| 统计每个客户的订单总数 | 聚合查询 |
| 查询总金额最高的订单 | 排序 + LIMIT |
| 列出所有待发货的订单 | WHERE 条件查询 |
| 查询北京客户的所有订单明细 | 多表 JOIN |

### API 调用

使用 `curl` 或 Postman 调用接口：

```bash
curl -X POST http://localhost:8081/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{
    "question": "列出所有客户的姓名"
  }'
```

**响应示例**：

```json
{
  "sql": "SELECT NAME FROM customers",
  "result": {
    "columns": ["NAME"],
    "rows": [
      ["张三"],
      ["李四"],
      ["王五"],
      ["赵六"],
      ["孙七"]
    ],
    "rowCount": 5
  },
  "explanation": "查询返回了 5 位客户的姓名。"
}
```

---

## 📚 API 文档

### 1. Text-to-SQL 接口

**端点**: `POST /agent/text2sql`

**请求体**:

```json
{
  "question": "自然语言问题"
}
```

**响应体**:

```json
{
  "sql": "生成的 SQL 语句",
  "result": {
    "columns": ["列名1", "列名2", ...],
    "rows": [
      [值1, 值2, ...],
      [值1, 值2, ...]
    ],
    "rowCount": 总行数
  },
  "explanation": "LLM 的自然语言解释"
}
```

**字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `sql` | String | LLM 生成并执行的 SQL 语句 |
| `result` | QueryResult | 结构化查询结果（来自 MCP Server） |
| `result.columns` | String[] | 列名数组 |
| `result.rows` | Object[][] | 数据行数组 |
| `result.rowCount` | int | 总行数 |
| `explanation` | String | LLM 对结果的自然语言解释 |

### 2. MCP Server 接口

#### schema.get - 获取表结构

**端点**: `POST /mcp/tools/schema.get`

**请求**:

```json
{
  "table": "customers"
}
```

**响应**:

```json
{
  "tableName": "customers",
  "columns": [
    {
      "name": "ID",
      "type": "BIGINT",
      "comment": "主键"
    },
    {
      "name": "NAME",
      "type": "VARCHAR(100)",
      "comment": "客户姓名"
    }
  ]
}
```

#### sql.run - 执行 SQL

**端点**: `POST /mcp/tools/sql.run`

**请求**:

```json
{
  "sql": "SELECT NAME FROM customers LIMIT 5"
}
```

**响应**:

```json
{
  "columns": ["NAME"],
  "rows": [
    ["张三"],
    ["李四"]
  ],
  "rowCount": 2
}
```

---

## ⚙️ 配置说明

### demo-sai 配置 (application.yml)

```yaml
server:
  port: 8081

spring:
  application:
    name: demo-sai

  # OpenAI 配置
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: gpt-4                 # 或 gpt-3.5-turbo
          temperature: 0.0             # 降低随机性，提高准确性
          max-tokens: 2000

# MCP Server 地址
mcp:
  server:
    base-url: http://localhost:9000

# 日志配置
logging:
  level:
    com.example.demo: INFO
    org.springframework.ai: DEBUG
```

### demo-saia 配置 (application.yml)

```yaml
server:
  port: 8082

spring:
  application:
    name: demo-saia

  # 阿里云通义千问配置
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-plus              # 或 qwen-turbo, qwen-max
          temperature: 0.0
          max-tokens: 2000

# MCP Server 地址
mcp:
  server:
    base-url: http://localhost:9000

# 日志配置
logging:
  level:
    com.example.demosaia: INFO
    org.springframework.ai: DEBUG
```

### MCP Server 配置 (application.yml)

```yaml
server:
  port: 9000

spring:
  application:
    name: mcp-server

  # H2 数据库配置
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:

  # SQL 初始化
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql

  # H2 控制台（可选）
  h2:
    console:
      enabled: true
      path: /h2-console
```

---

## 🎬 功能演示

### 1. 简单查询

**输入**：
```
列出所有客户的姓名和邮箱
```

**输出**：

**生成的 SQL**：
```sql
SELECT NAME, EMAIL FROM customers
```

**查询结果**：

| NAME | EMAIL |
|------|-------|
| 张三 | zhang@example.com |
| 李四 | li@example.com |
| 王五 | wang@example.com |

共 3 行

**结果解释**：
```
查询返回了 3 位客户的姓名和邮箱地址。
```

### 2. 聚合查询

**输入**：
```
统计每个客户的订单总数
```

**输出**：

**生成的 SQL**：
```sql
SELECT c.NAME, COUNT(o.ID) AS ORDER_COUNT
FROM customers c
LEFT JOIN orders o ON c.ID = o.CUSTOMER_ID
GROUP BY c.ID, c.NAME
ORDER BY ORDER_COUNT DESC
```

**查询结果**：

| NAME | ORDER_COUNT |
|------|-------------|
| 张三 | 2 |
| 李四 | 1 |
| 王五 | 0 |

共 3 行

**结果解释**：
```
张三有 2 笔订单，李四有 1 笔订单，王五暂无订单。
```

### 3. 多表 JOIN

**输入**：
```
查询北京客户的所有订单明细
```

**输出**：

**生成的 SQL**：
```sql
SELECT c.NAME, o.ORDER_DATE, oi.PRODUCT_NAME, oi.QUANTITY, oi.PRICE
FROM customers c
JOIN orders o ON c.ID = o.CUSTOMER_ID
JOIN order_items oi ON o.ID = oi.ORDER_ID
WHERE c.ADDRESS LIKE '%北京%'
ORDER BY o.ORDER_DATE DESC
```

---

## 🔍 技术细节

### 1. ThreadLocal 使用说明

**为什么使用 ThreadLocal？**

由于 Spring AI Function 的 `apply()` 方法只能返回 String，无法直接返回结构化对象，因此使用 `ThreadLocal` 跨方法传递 `QueryResult`：

```java
// McpToolService.java
private static final ThreadLocal<QueryResult> lastQueryResult = new ThreadLocal<>();

public String runSql(String sql) {
    // 调用 MCP Server
    Map<String, Object> response = mcpWebClient.post()...;

    // 保存结构化结果到 ThreadLocal
    saveQueryResult(response);

    // 返回 String（给 LLM 看的文本描述）
    return formatSqlResponse(response);
}

public static QueryResult getLastQueryResult() {
    return lastQueryResult.get();
}
```

**Text2SqlService 中获取**：

```java
// 调用 LLM + 工具
ChatResponse chatResponse = chatClient.prompt()...;

// 从 ThreadLocal 获取结构化结果
QueryResult queryResult = McpToolService.getLastQueryResult();

// 组装响应
return new Text2SqlResponse(sql, queryResult, explanation);
```

**注意事项**：
- ✅ 线程安全：每个 HTTP 请求使用独立线程
- ✅ 自动覆盖：下次请求会覆盖旧值
- ⚠️ 生产环境建议：使用 request-scope Bean 或 Context 传递

### 2. 前端表格限制策略

**为什么限制 20 行？**

防止大结果集导致页面卡顿：

```javascript
const maxRows = 20;
const displayRows = result.rows.slice(0, maxRows);

// 显示统计信息
const infoText = result.rowCount > maxRows
    ? `共 ${result.rowCount} 行，已展示前 ${displayRows.length} 行`
    : `共 ${result.rowCount} 行`;
```

**null 值处理**：

```javascript
const cellValue = cell !== null && cell !== undefined
    ? escapeHtml(String(cell))
    : '<i>null</i>';
```

### 3. LLM System Prompt 设计

```java
private static final String SYSTEM_PROMPT = """
    你是一个专业的数据库查询助手。请严格按照以下步骤操作：

    步骤 1: 分析用户问题，确定需要查询哪些表
    步骤 2: 调用 schemaGet 工具获取表结构（可多次调用）
    步骤 3: 根据表结构生成正确的 SQL（仅允许 SELECT 语句）
    步骤 4: 调用 sqlRun 工具执行 SQL
    步骤 5: 用中文简短解释查询结果（1-2 句话）

    可用表：
    - customers（客户表）：客户信息
    - orders（订单表）：订单记录
    - order_items（订单项表）：订单明细

    注意事项：
    - 必须先调用 schemaGet 再生成 SQL
    - SQL 必须基于实际字段名（大小写敏感）
    - 关联查询需要正确使用 JOIN
    - 最后返回简短的中文解释
    """;
```

**关键设计**：
1. ✅ **明确步骤**：减少 LLM 遗漏工具调用
2. ✅ **强制顺序**：先 schema 后 SQL，避免字段名错误
3. ✅ **限制操作**：只允许 SELECT，避免数据修改
4. ✅ **简短解释**：LLM 只负责总结，不列举所有数据

### 4. Spring AI 1.0.0 GA API

**统一的 ChatClient API**（demo-sai 与 demo-saia 完全相同）：

```java
ChatResponse chatResponse = chatClient.prompt()
    .system(SYSTEM_PROMPT)              // System Prompt
    .user(request.getQuestion())        // User Input
    .toolNames("schemaGet", "sqlRun")   // Function Names
    .call()
    .chatResponse();

String llmAnswer = chatResponse.getResult().getOutput().getText();
```

**Function 定义**（两个框架共享）：

```java
@Bean
@Description("获取数据库表结构，包括列名、类型和注释")
public Function<SchemaGetRequest, String> schemaGet() {
    return mcpToolService::getSchema;
}

@Bean
@Description("执行 SELECT 查询并返回结果")
public Function<SqlRunRequest, String> sqlRun() {
    return mcpToolService::runSql;
}
```
