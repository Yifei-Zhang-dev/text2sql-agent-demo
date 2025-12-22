# 问题修复说明

## 问题定位

**现象：**
- demo-sai 调用 MCP Server 失败
- AgentController 能收到请求，但 McpToolService.getSchema() 报错
- mcp-server 本身接口正常（浏览器访问 http://localhost:8083/mcp/tools 正常）

**根因分析：**
1. **localhost 解析问题**：Windows 环境下 localhost 可能解析到 IPv6 `::1`，导致连接失败
2. **错误日志不足**：原 WebClient 代码使用 `.retrieve().block()` 会吞掉详细错误信息
3. **缺少 Content-Type**：未显式设置 `application/json` 可能导致反序列化失败

## 修复方案

### 1. 修改 `application.yml`
```yaml
# 修改前
mcp:
  server:
    base-url: http://localhost:8083

# 修改后（使用 127.0.0.1 避免 IPv6 问题）
mcp:
  server:
    base-url: http://127.0.0.1:8083
```

### 2. 增强 `McpToolService` 错误日志
- 添加详细的请求/响应日志
- 使用 `.onStatus()` 捕获 HTTP 错误状态
- 使用 `.doOnError()` 记录异常信息
- 捕获 `WebClientResponseException` 输出状态码和响应体

**关键代码：**
```java
Map<String, Object> response = mcpWebClient.post()
        .uri("/mcp/tools/schema.get")
        .contentType(MediaType.APPLICATION_JSON)  // 显式设置 Content-Type
        .bodyValue(requestBody)
        .retrieve()
        .onStatus(status -> !status.is2xxSuccessful(),
                clientResponse -> {
                    log.error("HTTP 错误状态: {}", clientResponse.statusCode());
                    return clientResponse.bodyToMono(String.class)
                            .flatMap(body -> {
                                log.error("错误响应体: {}", body);
                                return Mono.error(new RuntimeException(
                                        "HTTP " + clientResponse.statusCode() + ": " + body));
                            });
                })
        .bodyToMono(Map.class)
        .doOnError(error -> log.error("请求失败: {}", error.getMessage()))
        .block();
```

### 3. 增强 `McpClientConfig` 日志
- 启动时打印 MCP Server Base URL
- 默认设置 Content-Type header

## 测试验证

### 1. 重启 demo-sai
```powershell
# Ctrl+C 停止当前运行的 demo-sai
$env:QWEN_API_KEY="your-api-key"
cd D:\projects\spring-ai-demo\demo-sai
..\mvnw.cmd spring-boot:run
```

### 2. 查看启动日志
应该看到：
```
=== 初始化 MCP WebClient ===
MCP Server Base URL: http://127.0.0.1:8083
```

### 3. 测试 Web UI
访问 http://localhost:8081/
输入问题："查询所有客户的姓名和邮箱"
点击"提交查询"

### 4. 查看详细日志
应该看到：
```
=== Text-to-SQL 请求开始 ===
用户问题: 查询所有客户的姓名和邮箱
正在调用 LLM...
=== 调用 MCP Tool: schema.get ===
请求参数: table=customers
请求体: {table=customers}
schema.get 成功响应: {tableName=CUSTOMERS, columns=[...]}
...
```

## 预期结果

- ✅ MCP Tool 调用成功
- ✅ 获取到表结构信息
- ✅ LLM 生成正确 SQL
- ✅ SQL 执行成功
- ✅ Web UI 显示：生成的 SQL、查询结果、解释

## 如仍有问题

请提供：
1. demo-sai 完整启动日志
2. 测试时的完整错误日志（包括 HTTP 状态码和响应体）
3. mcp-server 端的日志（看是否收到请求）
