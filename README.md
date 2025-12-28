# Spring AI vs Spring AI Alibaba - Text-to-SQL Demo

这是一个用于技术选型 PPT 的标准化演示项目，展示 **Spring AI** 和 **Spring AI Alibaba** 在 Text-to-SQL 场景下的能力对比。

## 项目概述

本项目包含 3 个独立模块：
1. **mcp-server**: MCP (Model Context Protocol) Server，提供数据库工具（schemaGet、sqlRun）
2. **demo-sai**: 基于 **Spring AI 1.0.0 GA** 的 Text-to-SQL 实现
3. **demo-saia**: 基于 **Spring AI Alibaba 1.0.0.2 GA** 的 Text-to-SQL 实现

### 技术栈

| 模块 | Spring Boot | Spring AI | Spring AI Alibaba | LLM Provider |
|------|-------------|-----------|-------------------|--------------|
| **demo-sai** | 3.5.9 | **1.0.0 GA** | - | DashScope (OpenAI-compatible) |
| **demo-saia** | 3.5.9 | 1.0.0 GA | **1.0.0.2 GA** | DashScope (原生) |
| **mcp-server** | 3.5.9 | - | - | H2 Database |

### 架构图

```
┌─────────────┐         ┌──────────────┐
│  demo-sai   │         │  demo-saia   │
│  (8081)     │         │  (8082)      │
│             │         │              │
│ Spring AI   │         │ Spring AI    │
│ 1.0.0 GA    │         │ Alibaba      │
│             │         │ 1.0.0.2 GA   │
└──────┬──────┘         └──────┬───────┘
       │                       │
       │    HTTP Tool Calls    │
       └───────────┬───────────┘
                   │
            ┌──────▼──────┐
            │ MCP Server  │
            │  (8083)     │
            │             │
            │ Tools:      │
            │ - schemaGet │
            │ - sqlRun    │
            └─────────────┘
```

---

## 快速开始

### 前置条件

1. **JDK 17+**
2. **Maven 3.8+**
3. **阿里云 DashScope API Key** ([获取地址](https://dashscope.console.aliyun.com/apiKey))

### 启动步骤

#### 1. 设置环境变量

**Linux/macOS**:
```bash
export QWEN_API_KEY="sk-your-api-key-here"
```

**Windows PowerShell**:
```powershell
$env:QWEN_API_KEY="sk-your-api-key-here"
```

**Windows CMD**:
```cmd
set QWEN_API_KEY=sk-your-api-key-here
```

#### 2. 启动 MCP Server（必须第一个启动）

```bash
cd mcp-server
mvn spring-boot:run
```

等待日志输出：
```
Started McpServerApplication in X.XXX seconds (process running for X.XXX)
```

验证：
```bash
curl http://localhost:8083/actuator/health
# 预期输出: {"status":"UP"}
```

#### 3. 启动 demo-sai（Spring AI 版本）

**新开终端窗口**:
```bash
cd demo-sai
mvn spring-boot:run
```

等待日志输出：
```
初始化 ChatClient (Spring AI)
Started DemoApplication in X.XXX seconds
```

验证：
```bash
curl http://localhost:8081/actuator/health
# 预期输出: {"status":"UP"}
```

#### 4. 启动 demo-saia（Spring AI Alibaba 版本）

**新开终端窗口**:
```bash
cd demo-saia
mvn spring-boot:run
```

等待日志输出：
```
初始化 ChatClient (Spring AI Alibaba)
Started DemoSaiaApplication in X.XXX seconds
```

验证：
```bash
curl http://localhost:8082/actuator/health
# 预期输出: {"status":"UP"}
```

---

## 测试用例

完整的测试用例请参考 [TEST_CASES.md](./TEST_CASES.md)。

### 快速测试

**测试 demo-sai (Spring AI)**:
```bash
curl -X POST http://localhost:8081/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{"question":"查询所有客户的姓名和邮箱"}'
```

**测试 demo-saia (Spring AI Alibaba)**:
```bash
curl -X POST http://localhost:8082/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{"question":"查询所有客户的姓名和邮箱"}'
```

**PowerShell 测试**:
```powershell
$body = @{question = "查询所有客户的姓名和邮箱"} | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8081/agent/text2sql" -Method Post -ContentType "application/json" -Body $body | ConvertTo-Json -Depth 10
```

### 预期响应格式

```json
{
  "executedSql": "SELECT name, email FROM customers;",
  "llmAnswer": "查询成功，共返回 10 条客户记录。",
  "status": "查询完成"
}
```

---

## API 文档

### demo-sai (Spring AI)

- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **API Docs**: http://localhost:8081/v3/api-docs

### demo-saia (Spring AI Alibaba)

- **Swagger UI**: http://localhost:8082/swagger-ui.html
- **API Docs**: http://localhost:8082/v3/api-docs

### MCP Server

- **Swagger UI**: http://localhost:8083/swagger-ui.html

---

## 项目结构

### 统一的代码结构（demo-sai 和 demo-saia）

```
src/main/java/com/example/
├── controller/
│   ├── AgentController.java        # HTTP 入口，调用 Service
│   └── DiagnosticController.java   # 连通性检查
├── service/
│   ├── Text2SqlService.java        # 核心业务逻辑（System Prompt + Tool Calling）
│   └── McpToolService.java         # MCP Server 工具调用 + SQL 记录
├── config/
│   ├── ChatClientConfig.java       # ChatClient Bean 配置
│   ├── McpFunctionConfig.java      # 工具函数注册
│   └── McpClientConfig.java        # WebClient 配置
└── dto/
    ├── Text2SqlRequest.java        # 请求 DTO
    └── Text2SqlResponse.java       # 响应 DTO（含 executedSql、llmAnswer、status）
```

### 关键差异点

| 文件 | demo-sai (Spring AI) | demo-saia (Spring AI Alibaba) |
|------|---------------------|------------------------------|
| `pom.xml` | `spring-ai-starter-model-openai` | `spring-ai-alibaba-starter-dashscope` |
| `application.yml` | `spring.ai.openai.*` | `spring.ai.dashscope.*` |
| `ChatClientConfig` | 日志：`(Spring AI)` | 日志：`(Spring AI Alibaba)` |
| `Text2SqlService` | 日志：`(Spring AI)` | 日志：`(Spring AI Alibaba)` |

---

## 数据库说明

### 表结构

MCP Server 使用 H2 内存数据库，包含 3 张表：

1. **customers**（客户表）
   - `id`: 主键
   - `name`: 客户姓名
   - `email`: 电子邮箱
   - `phone`: 联系电话

2. **orders**（订单表）
   - `id`: 主键
   - `customer_id`: 外键（关联 customers）
   - `order_date`: 订单日期
   - `total_amount`: 订单总金额
   - `status`: 订单状态（pending/paid/shipped/completed）

3. **order_items**（订单项表）
   - `id`: 主键
   - `order_id`: 外键（关联 orders）
   - `product_name`: 商品名称
   - `quantity`: 购买数量
   - `unit_price`: 单价
   - `subtotal`: 小计金额

### 测试数据

- **10 个客户**（张三、李四、王五...）
- **15 个订单**（总金额约 148,440 元）
- **30+ 个订单项**（涵盖 MacBook、iPhone、iPad、AirPods 等产品）

---

## 故障排查

### 1. MCP Server 未启动

**错误**:
```
Connection refused: http://localhost:8083
```

**解决**:
```bash
cd mcp-server
mvn spring-boot:run
# 确保日志中看到 "Started McpServerApplication"
```

### 2. API Key 未设置

**错误**:
```
401 Unauthorized
```

**解决**:
```bash
export QWEN_API_KEY="sk-your-actual-key"
```

### 3. 端口被占用

**错误**:
```
Port 8081 was already in use
```

**解决**:
```bash
# 查找并停止占用端口的进程
lsof -ti:8081 | xargs kill -9  # macOS/Linux
netstat -ano | findstr :8081   # Windows（手动杀进程）
```

### 4. 工具调用失败

**错误**:
```
Tool execution failed: schemaGet
```

**检查**:
1. MCP Server 是否在 8083 端口运行
2. `application.yml` 中 `mcp.server.base-url` 是否正确
3. 查看 MCP Server 日志是否有报错

### 5. SQL 生成错误

**可能原因**:
- LLM 未正确理解表结构
- System Prompt 不够清晰

**调试方法**:
1. 查看 demo 控制台日志中的 `schemaGet` 调用
2. 检查 `executedSql` 是否符合预期
3. 调整 `Text2SqlService.java` 中的 `SYSTEM_PROMPT`

---

## 依赖版本验证

### 验证无版本混用

**demo-sai**:
```bash
cd demo-sai
mvn dependency:tree -Dincludes=org.springframework.ai
```

**预期**：所有 `org.springframework.ai:*` 依赖都是 **1.0.0**

**demo-saia**:
```bash
cd demo-saia
mvn dependency:tree -Dincludes=org.springframework.ai,com.alibaba.cloud.ai
```

**预期**：
- `org.springframework.ai:*` 都是 **1.0.0**
- `com.alibaba.cloud.ai:*` 都是 **1.0.0.2**

---

## 技术选型对比

详细对比请参考 [COMPARISON.md](./COMPARISON.md)。

### 核心差异总结

| 维度 | Spring AI | Spring AI Alibaba |
|------|-----------|-------------------|
| **版本** | 1.0.0 GA | 1.0.0.2 GA (基于 Spring AI 1.0.0) |
| **官方支持** | Spring 官方 | 阿里云官方 |
| **LLM Provider** | OpenAI-compatible | DashScope 原生 |
| **配置前缀** | `spring.ai.openai.*` | `spring.ai.dashscope.*` |
| **API 一致性** | 统一使用 Spring AI 1.0.0 API（`.toolNames()`） |
| **Tool Calling** | ✅ 支持 | ✅ 支持 |
| **适用场景** | 跨 Provider（OpenAI/Azure/Anthropic...） | 阿里云 DashScope 深度集成 |

---

## 参考资料

- [Spring AI 1.0.0 GA 发布公告](https://spring.io/blog/2025/05/20/spring-ai-1-0-GA-released/)
- [Spring AI Alibaba 1.0 GA 发布公告](https://www.alibabacloud.com/blog/spring-ai-alibaba-1-0-ga-officially-released-marking-the-advent-of-a-new-era-in-java-agent-development_602299)
- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Spring AI Alibaba 官方文档](https://java2ai.com/)
- [阿里云 DashScope API 文档](https://help.aliyun.com/zh/dashscope/)

---

## 许可证

本项目仅用于技术选型演示和学习目的。

## 联系方式

如有问题，请查阅 [TEST_CASES.md](./TEST_CASES.md) 中的故障排查部分。
