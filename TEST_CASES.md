# Text-to-SQL Demo 测试用例（PPT 演示专用）

本文档包含 6 个精心设计的测试用例，用于展示 Spring AI 和 Spring AI Alibaba 的 Text-to-SQL 能力。
每个用例都包含 curl 和 PowerShell 两种调用方式。

---

## 环境准备

### 1. 启动顺序
```bash
# 1. 启动 MCP Server (端口 8083)
cd mcp-server
mvn spring-boot:run

# 2. 启动 demo-sai (端口 8081) - Spring AI 版本
cd demo-sai
export QWEN_API_KEY="your-api-key"
mvn spring-boot:run

# 3. 启动 demo-saia (端口 8082) - Spring AI Alibaba 版本
cd demo-saia
export QWEN_API_KEY="your-api-key"
mvn spring-boot:run
```

### 2. 健康检查
```bash
# MCP Server
curl http://localhost:8083/actuator/health

# demo-sai
curl http://localhost:8081/actuator/health

# demo-saia
curl http://localhost:8082/actuator/health
```

---

## 测试用例

### 用例 1：基础查询 - 查询所有客户姓名和邮箱

**业务场景**：获取客户联系信息列表

**curl (demo-sai)**:
```bash
curl -X POST http://localhost:8081/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{"question":"查询所有客户的姓名和邮箱"}'
```

**curl (demo-saia)**:
```bash
curl -X POST http://localhost:8082/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{"question":"查询所有客户的姓名和邮箱"}'
```

**PowerShell (demo-sai)**:
```powershell
$body = @{
    question = "查询所有客户的姓名和邮箱"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/agent/text2sql" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body | ConvertTo-Json -Depth 10
```

**PowerShell (demo-saia)**:
```powershell
$body = @{
    question = "查询所有客户的姓名和邮箱"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/agent/text2sql" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body | ConvertTo-Json -Depth 10
```

**预期 SQL**:
```sql
SELECT name, email FROM customers;
```

**预期结果**：返回 10 条客户记录

---

### 用例 2：聚合查询 - 每个客户的订单总数

**业务场景**：统计客户活跃度（涉及 JOIN + GROUP BY）

**curl (demo-sai)**:
```bash
curl -X POST http://localhost:8081/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{"question":"统计每个客户的订单总数"}'
```

**curl (demo-saia)**:
```bash
curl -X POST http://localhost:8082/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{"question":"统计每个客户的订单总数"}'
```

**PowerShell (demo-sai)**:
```powershell
$body = @{
    question = "统计每个客户的订单总数"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/agent/text2sql" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body | ConvertTo-Json -Depth 10
```

**PowerShell (demo-saia)**:
```powershell
$body = @{
    question = "统计每个客户的订单总数"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/agent/text2sql" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body | ConvertTo-Json -Depth 10
```

**预期 SQL**:
```sql
SELECT c.name, COUNT(o.id) as order_count
FROM customers c
LEFT JOIN orders o ON c.id = o.customer_id
GROUP BY c.name;
```

**预期结果**：展示 10 个客户的订单数量（张三 3 个，李四 2 个，王五 2 个等）

---

### 用例 3：聚合计算 - 所有订单的总金额

**业务场景**：计算销售总额（涉及 SUM）

**curl (demo-sai)**:
```bash
curl -X POST http://localhost:8081/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{"question":"计算所有订单的总金额是多少"}'
```

**curl (demo-saia)**:
```bash
curl -X POST http://localhost:8082/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{"question":"计算所有订单的总金额是多少"}'
```

**PowerShell (demo-sai)**:
```powershell
$body = @{
    question = "计算所有订单的总金额是多少"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/agent/text2sql" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body | ConvertTo-Json -Depth 10
```

**PowerShell (demo-saia)**:
```powershell
$body = @{
    question = "计算所有订单的总金额是多少"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/agent/text2sql" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body | ConvertTo-Json -Depth 10
```

**预期 SQL**:
```sql
SELECT SUM(total_amount) as total_sales FROM orders;
```

**预期结果**：返回总金额（约 148,440.00）

---

### 用例 4：排序查询 - 金额最高的订单

**业务场景**：查找 VIP 订单（涉及 ORDER BY + LIMIT + JOIN）

**curl (demo-sai)**:
```bash
curl -X POST http://localhost:8081/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{"question":"查询金额最高的订单，包含客户姓名"}'
```

**curl (demo-saia)**:
```bash
curl -X POST http://localhost:8082/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{"question":"查询金额最高的订单，包含客户姓名"}'
```

**PowerShell (demo-sai)**:
```powershell
$body = @{
    question = "查询金额最高的订单，包含客户姓名"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/agent/text2sql" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body | ConvertTo-Json -Depth 10
```

**PowerShell (demo-saia)**:
```powershell
$body = @{
    question = "查询金额最高的订单，包含客户姓名"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/agent/text2sql" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body | ConvertTo-Json -Depth 10
```

**预期 SQL**:
```sql
SELECT c.name, o.total_amount, o.order_date
FROM orders o
JOIN customers c ON o.customer_id = c.id
ORDER BY o.total_amount DESC
LIMIT 1;
```

**预期结果**：王五，24997.00，2024-01-17

---

### 用例 5：条件查询 - 已完成订单的客户

**业务场景**：筛选特定状态的订单（涉及 WHERE + DISTINCT）

**curl (demo-sai)**:
```bash
curl -X POST http://localhost:8081/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{"question":"查询所有状态为已完成的订单对应的客户姓名"}'
```

**curl (demo-saia)**:
```bash
curl -X POST http://localhost:8082/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{"question":"查询所有状态为已完成的订单对应的客户姓名"}'
```

**PowerShell (demo-sai)**:
```powershell
$body = @{
    question = "查询所有状态为已完成的订单对应的客户姓名"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/agent/text2sql" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body | ConvertTo-Json -Depth 10
```

**PowerShell (demo-saia)**:
```powershell
$body = @{
    question = "查询所有状态为已完成的订单对应的客户姓名"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/agent/text2sql" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body | ConvertTo-Json -Depth 10
```

**预期 SQL**:
```sql
SELECT DISTINCT c.name
FROM customers c
JOIN orders o ON c.id = o.customer_id
WHERE o.status = 'completed';
```

**预期结果**：返回 8 个客户（张三、李四、王五、赵六、孙七、周八、陈一）

---

### 用例 6：复杂多表查询 - 购买了 MacBook 系列产品的客户

**业务场景**：产品销售分析（涉及 3 表 JOIN + LIKE）

**curl (demo-sai)**:
```bash
curl -X POST http://localhost:8081/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{"question":"查询购买过MacBook产品的客户姓名和他们购买的MacBook型号"}'
```

**curl (demo-saia)**:
```bash
curl -X POST http://localhost:8082/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{"question":"查询购买过MacBook产品的客户姓名和他们购买的MacBook型号"}'
```

**PowerShell (demo-sai)**:
```powershell
$body = @{
    question = "查询购买过MacBook产品的客户姓名和他们购买的MacBook型号"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/agent/text2sql" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body | ConvertTo-Json -Depth 10
```

**PowerShell (demo-saia)**:
```powershell
$body = @{
    question = "查询购买过MacBook产品的客户姓名和他们购买的MacBook型号"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/agent/text2sql" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body | ConvertTo-Json -Depth 10
```

**预期 SQL**:
```sql
SELECT DISTINCT c.name, oi.product_name
FROM customers c
JOIN orders o ON c.id = o.customer_id
JOIN order_items oi ON o.id = oi.order_id
WHERE oi.product_name LIKE '%MacBook%';
```

**预期结果**：返回 5 个客户购买的 MacBook 产品（张三-MacBook Pro 14, 李四-MacBook Air, 王五-MacBook Pro 16, 周八-MacBook Pro 16, 刘二-MacBook Air M2）

---

## 演示建议

### PPT 截图要点
1. **日志清晰度**：展示控制台中的 `schemaGet` → `sqlRun` 调用链
2. **JSON 响应**：展示 `executedSql` 和 `llmAnswer` 字段
3. **对比性**：同一用例在两个 demo 上并排展示（响应时间、Token 用量）

### 演示顺序
1. 从简单到复杂：用例 1 → 用例 2 → 用例 3 → 用例 4 → 用例 5 → 用例 6
2. 每个用例先演示 demo-sai，再演示 demo-saia
3. 最后展示对比总结（性能、易用性、功能差异）

---

## 故障排查

### 常见问题

**1. MCP Server 未启动**
```
错误: Connection refused
解决: 确保 MCP Server 在 8083 端口运行
```

**2. API Key 未设置**
```
错误: 401 Unauthorized
解决: export QWEN_API_KEY="your-api-key"
```

**3. 工具调用失败**
```
错误: Tool execution failed
解决: 检查 application.yml 中的 mcp.server.base-url
```

**4. SQL 语法错误**
```
错误: SQL syntax error
解决: 检查 data.sql 是否正确初始化
```

---

## 附录：快速测试脚本

### Bash 脚本（测试所有用例）
```bash
#!/bin/bash
PORT=$1  # 8081 for demo-sai, 8082 for demo-saia

echo "=== 测试用例 1: 查询所有客户 ==="
curl -s -X POST http://localhost:$PORT/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{"question":"查询所有客户的姓名和邮箱"}' | jq .

echo -e "\n=== 测试用例 2: 订单总数 ==="
curl -s -X POST http://localhost:$PORT/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{"question":"统计每个客户的订单总数"}' | jq .

echo -e "\n=== 测试用例 3: 总金额 ==="
curl -s -X POST http://localhost:$PORT/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{"question":"计算所有订单的总金额是多少"}' | jq .

echo -e "\n=== 测试用例 4: 最高订单 ==="
curl -s -X POST http://localhost:$PORT/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{"question":"查询金额最高的订单，包含客户姓名"}' | jq .

echo -e "\n=== 测试用例 5: 已完成订单 ==="
curl -s -X POST http://localhost:$PORT/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{"question":"查询所有状态为已完成的订单对应的客户姓名"}' | jq .

echo -e "\n=== 测试用例 6: MacBook 客户 ==="
curl -s -X POST http://localhost:$PORT/agent/text2sql \
  -H "Content-Type: application/json" \
  -d '{"question":"查询购买过MacBook产品的客户姓名和他们购买的MacBook型号"}' | jq .
```

**使用方法**:
```bash
# 测试 demo-sai
./test_all.sh 8081

# 测试 demo-saia
./test_all.sh 8082
```

### PowerShell 脚本（测试所有用例）
```powershell
param(
    [Parameter(Mandatory=$true)]
    [int]$Port  # 8081 for demo-sai, 8082 for demo-saia
)

$testCases = @(
    @{name="查询所有客户"; question="查询所有客户的姓名和邮箱"},
    @{name="订单总数"; question="统计每个客户的订单总数"},
    @{name="总金额"; question="计算所有订单的总金额是多少"},
    @{name="最高订单"; question="查询金额最高的订单，包含客户姓名"},
    @{name="已完成订单"; question="查询所有状态为已完成的订单对应的客户姓名"},
    @{name="MacBook客户"; question="查询购买过MacBook产品的客户姓名和他们购买的MacBook型号"}
)

foreach ($test in $testCases) {
    Write-Host "`n=== 测试用例: $($test.name) ===" -ForegroundColor Cyan

    $body = @{question = $test.question} | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "http://localhost:$Port/agent/text2sql" `
        -Method Post `
        -ContentType "application/json" `
        -Body $body

    $response | ConvertTo-Json -Depth 10 | Write-Host
}
```

**使用方法**:
```powershell
# 测试 demo-sai
.\test_all.ps1 -Port 8081

# 测试 demo-saia
.\test_all.ps1 -Port 8082
```
