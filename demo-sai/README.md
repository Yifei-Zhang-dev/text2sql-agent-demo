# Demo-SAI (Spring AI)

基于 Spring AI 实现的 Text-to-SQL Agent 演示项目。

## 技术栈

- Spring Boot 3.5.9
- Spring AI 1.0.0-M5（OpenAI-compatible）
- 阿里云 DashScope / Qwen
- WebFlux（HTTP 客户端）

## 前置条件

1. **MCP Server 必须先启动**（端口 8083）
2. **配置 QWEN_API_KEY 环境变量**

### IDEA Run Configuration

在 `Environment variables` 中设置：

```
QWEN_API_KEY=sk-your-dashscope-api-key-here
```

## 快速启动

```powershell
# 确保 MCP Server 已启动（另一个窗口）
cd ../mcp-server
.\mvnw.cmd spring-boot:run

# 启动 demo-sai
cd demo-sai
.\mvnw.cmd spring-boot:run
```

## 测试接口

### 1. 健康检查

```powershell
curl http://localhost:8081/actuator/health
```

### 2. 诊断接口

```powershell
# 检查配置
curl http://localhost:8081/diagnostic/config

# 测试 LLM 连通性
curl http://localhost:8081/diagnostic/test-simple
```

### 3. Text-to-SQL 示例

**推荐使用方式（PowerShell）：**

#### 示例 1: 简单查询

```powershell
$body = @{question="查询所有客户的姓名"} | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8081/agent/text2sql" -Method Post -ContentType "application/json; charset=utf-8" -Body $body
```

**预期行为：**
1. LLM 调用 `schemaGet("customers")`
2. 生成 SQL: `SELECT name FROM customers`
3. 调用 `sqlRun(sql)`
4. 返回结果并解释

#### 示例 2: 聚合查询

```powershell
$body = @{question="订单总金额是多少"} | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8081/agent/text2sql" -Method Post -ContentType "application/json; charset=utf-8" -Body $body
```

**预期 SQL:** `SELECT SUM(total_amount) FROM orders`

#### 示例 3: 关联查询

```powershell
curl -X POST http://localhost:8081/agent/text2sql `
  -H "Content-Type: application/json" `
  -d '{\"question\":\"查询张三的所有订单\"}'
```

**预期 SQL:**
```sql
SELECT o.*
FROM customers c
JOIN orders o ON c.id = o.customer_id
WHERE c.name = '张三'
```

#### 示例 4: 复杂聚合

```powershell
curl -X POST http://localhost:8081/agent/text2sql `
  -H "Content-Type: application/json" `
  -d '{\"question\":\"每个客户的订单总数\"}'
```

**预期 SQL:**
```sql
SELECT c.name, COUNT(o.id) as order_count
FROM customers c
LEFT JOIN orders o ON c.id = o.customer_id
GROUP BY c.id, c.name
```

## 日志查看

启动后查看控制台日志，会显示完整的工具调用流程：

```
========== Text-to-SQL 请求开始 ==========
用户问题: 查询所有客户的姓名
正在调用 LLM...
调用 MCP Tool: schema.get, table=customers
schema.get 响应: {...}
调用 MCP Tool: sql.run, sql=SELECT name FROM customers
sql.run 响应: {...}
LLM 最终响应: 查询结果显示...
Token 使用情况: {...}
========== Text-to-SQL 请求完成 ==========
```

## 常见问题

### Q1: 启动报 401 Unauthorized

**检查：**
- `QWEN_API_KEY` 环境变量是否正确设置
- API Key 是否有效（访问 `http://localhost:8081/diagnostic/config` 验证）

### Q2: MCP Server 调用失败

**检查：**
- MCP Server 是否已启动（端口 8083）
- 访问 `http://localhost:8083/actuator/health` 验证

### Q3: LLM 未调用工具

**可能原因：**
- Prompt 不够明确
- 模型未正确识别需要查询数据库
- 尝试更明确的问题，如"查询数据库中的客户信息"

## 对比要点（用于 PPT）

| 维度 | 实现方式 |
|------|----------|
| **LLM 集成** | Spring AI OpenAI Starter |
| **API 兼容性** | OpenAI-compatible（支持 DashScope） |
| **Function Calling** | 自动注册 `@Bean` 函数 |
| **工具调用** | WebClient HTTP 调用 MCP Server |
| **日志可观测性** | 详细的结构化日志 |
| **依赖数量** | 3 个（Web + AI + WebFlux） |

## 下一步

- 查看 `../demo-saia` 对比 Spring AI Alibaba 实现
- 查看日志分析工具调用稳定性
- 测试更复杂的查询场景
