# 修复验证清单

## 已修改的文件

1. ✅ `demo-sai/src/main/resources/application.yml`
   - 修改：`base-url: http://localhost:8083` → `http://127.0.0.1:8083`
   - 原因：避免 Windows 环境 localhost 解析到 IPv6

2. ✅ `demo-sai/src/main/java/com/example/demo/service/McpToolService.java`
   - 新增：详细的请求/响应日志
   - 新增：`.contentType(MediaType.APPLICATION_JSON)` 显式设置 Content-Type
   - 新增：`.onStatus()` 捕获 HTTP 错误并打印状态码和响应体
   - 新增：`.doOnError()` 记录异常
   - 新增：`WebClientResponseException` 异常处理，打印完整错误信息

3. ✅ `demo-sai/src/main/java/com/example/demo/config/McpClientConfig.java`
   - 新增：启动时打印 MCP Server Base URL
   - 新增：默认 Content-Type header

## 重启验证步骤

### Step 1: 确认 mcp-server 运行中
```powershell
# 测试 mcp-server 接口
Invoke-RestMethod -Uri "http://127.0.0.1:8083/mcp/tools" -Method Get
```

预期返回：工具列表 JSON

### Step 2: 重启 demo-sai
```powershell
# Ctrl+C 停止当前 demo-sai
$env:QWEN_API_KEY="sk-xxx"
cd D:\projects\spring-ai-demo\demo-sai
..\mvnw.cmd spring-boot:run
```

### Step 3: 查看启动日志
应该看到：
```
=== 初始化 MCP WebClient ===
MCP Server Base URL: http://127.0.0.1:8083
```

### Step 4: 测试 Web UI
1. 浏览器访问：http://localhost:8081/
2. 输入问题："查询所有客户的姓名和邮箱"
3. 点击"提交查询"

### Step 5: 查看日志（成功场景）
```
========== Text-to-SQL 请求开始 ==========
用户问题: 查询所有客户的姓名和邮箱
正在调用 LLM...
=== 调用 MCP Tool: schema.get ===
请求参数: table=customers
请求体: {table=customers}
schema.get 成功响应: {tableName=CUSTOMERS, columns=[...]}
=== 调用 MCP Tool: sql.run ===
SQL: SELECT name, email FROM customers
请求体: {sql=SELECT name, email FROM customers}
sql.run 成功响应: {columns=[NAME, EMAIL], rows=[[Alice, alice@example.com], ...], rowCount=5}
LLM 最终响应: 查询结果显示有5位客户...
Token 使用情况: ...
========== Text-to-SQL 请求完成 ==========
```

### Step 6: 验证 Web UI 显示
应该显示：
- ✅ **生成的 SQL**: `SELECT name, email FROM customers`
- ✅ **查询结果**: 客户列表（JSON 格式）
- ✅ **结果解释**: LLM 的中文解释

## 如果仍然失败

查看日志中的错误信息：

### 场景 1: 连接失败
```
=== schema.get 调用异常 ===
java.net.ConnectException: Connection refused
```
**解决方案**：检查 mcp-server 是否在 8083 端口运行

### 场景 2: HTTP 错误
```
=== schema.get HTTP 错误 ===
状态码: 404
响应体: ...
```
**解决方案**：检查 mcp-server 的路由配置

### 场景 3: JSON 解析失败
```
请求失败: Could not extract response
```
**解决方案**：检查 mcp-server 返回的 JSON 格式

## 成功标志

- ✅ 无异常日志
- ✅ 看到 "schema.get 成功响应"
- ✅ 看到 "sql.run 成功响应"
- ✅ Web UI 显示完整结果
- ✅ LLM Token 使用情况正常打印
