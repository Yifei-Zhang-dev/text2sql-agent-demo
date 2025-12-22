# 🔧 MCP 调用问题修复总结

## 问题描述
demo-sai 调用 MCP Server 失败，无法获取表结构信息，导致 Text-to-SQL 功能无法正常工作。

## 根本原因
1. **localhost 解析问题**：Windows 环境下可能解析到 IPv6 `::1`，而 mcp-server 监听在 IPv4
2. **错误信息被吞没**：原 WebClient 代码没有捕获详细的 HTTP 错误信息
3. **缺少请求头**：未显式设置 `Content-Type: application/json`

## 修复内容

### ✅ 修改 1: application.yml
```yaml
# 位置: demo-sai/src/main/resources/application.yml
mcp:
  server:
    base-url: http://127.0.0.1:8083  # 从 localhost 改为 127.0.0.1
```

### ✅ 修改 2: McpToolService.java
增强错误诊断能力：
- ✅ 添加详细的请求/响应日志（请求参数、请求体、响应体）
- ✅ 显式设置 `Content-Type: application/json`
- ✅ 使用 `.onStatus()` 捕获 HTTP 错误状态码和响应体
- ✅ 使用 `.doOnError()` 记录异常信息
- ✅ 捕获 `WebClientResponseException` 输出完整错误

**关键代码片段：**
```java
.contentType(MediaType.APPLICATION_JSON)
.retrieve()
.onStatus(status -> !status.is2xxSuccessful(),
        clientResponse -> {
            log.error("HTTP 错误状态: {}", clientResponse.statusCode());
            return clientResponse.bodyToMono(String.class)
                    .flatMap(body -> {
                        log.error("错误响应体: {}", body);
                        return Mono.error(new RuntimeException(...));
                    });
        })
.doOnError(error -> log.error("请求失败: {}", error.getMessage()))
```

### ✅ 修改 3: McpClientConfig.java
增加启动诊断：
- ✅ 启动时打印 MCP Server Base URL
- ✅ 默认设置 `Content-Type` header

## 验证步骤

### 1. 重启 demo-sai
```powershell
# 停止当前运行的 demo-sai (Ctrl+C)
$env:QWEN_API_KEY="your-api-key"
cd D:\projects\spring-ai-demo\demo-sai
..\mvnw.cmd spring-boot:run
```

### 2. 查看启动日志
✅ 应该看到：
```
=== 初始化 MCP WebClient ===
MCP Server Base URL: http://127.0.0.1:8083
```

### 3. 测试 Web UI
1. 访问：http://localhost:8081/
2. 输入："查询所有客户的姓名和邮箱"
3. 点击"提交查询"

### 4. 查看成功日志
✅ 应该看到：
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
sql.run 成功响应: {columns=[NAME, EMAIL], rows=[[...]], rowCount=5}
LLM 最终响应: ...
========== Text-to-SQL 请求完成 ==========
```

### 5. 验证 Web UI
✅ 应该显示：
- **生成的 SQL**: `SELECT name, email FROM customers`
- **查询结果**: 客户数据（JSON 格式）
- **结果解释**: LLM 的中文解释

## 如何诊断新问题

现在有详细日志，如果出现问题会清晰显示：

### 连接失败
```
=== schema.get 调用异常 ===
java.net.ConnectException: Connection refused
```
→ 检查 mcp-server 是否运行在 8083 端口

### HTTP 错误
```
=== schema.get HTTP 错误 ===
状态码: 404
响应体: {"error": "..."}
```
→ 检查 URL 路径是否正确

### 超时
```
请求失败: Timeout on blocking read
```
→ 检查网络或增加超时时间

## 文件清单

修改的文件：
1. `demo-sai/src/main/resources/application.yml`
2. `demo-sai/src/main/java/com/example/demo/service/McpToolService.java`
3. `demo-sai/src/main/java/com/example/demo/config/McpClientConfig.java`

新增的文档：
1. `TROUBLESHOOTING.md` - 详细问题分析
2. `verify-fix.md` - 验证清单
3. `FIX-SUMMARY.md` - 本文件

## 下一步

如果验证成功，可以继续 Commit 6，包含：
- Web UI 功能
- Swagger UI 文档
- MCP 调用修复

---

**修复完成时间**: 2025-12-22
**修复方法**: 直接定位根因 + 增强错误诊断
