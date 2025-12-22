# 调试 MCP Server 500 错误

## 当前状态

✅ **已修复**：
- demo-sai → mcp-server 的连接问题（localhost → 127.0.0.1）
- WebClient 错误日志增强

❌ **新问题**：
- mcp-server 返回 HTTP 500 Internal Server Error
- 错误路径：`/mcp/tools/schema.get`
- demo-sai 日志显示：
  ```
  HTTP 错误状态: 500 INTERNAL_SERVER_ERROR
  错误响应体: {"timestamp":"...","status":500,"error":"Internal Server Error","path":"/mcp/tools/schema.get"}
  ```

## 需要检查 mcp-server 端日志

### 重启 mcp-server（带详细日志）

mcp-server 已添加详细日志，需要重启以查看错误详情：

```powershell
# 1. 停止当前 mcp-server (Ctrl+C)

# 2. 重启 mcp-server
cd D:\projects\spring-ai-demo\mcp-server
..\mvnw.cmd spring-boot:run

# 3. 等待启动完成，应该看到：
# Started McpServerApplication in X.XXX seconds

# 4. 测试接口
Invoke-RestMethod -Uri "http://127.0.0.1:8083/mcp/tools" -Method Get
```

### 触发错误并查看日志

```powershell
# 在 Web UI 中提交问题："查询所有客户的姓名和邮箱"
# 或使用 PowerShell：
$body = @{question="查询所有客户的姓名和邮箱"} | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8081/agent/text2sql" -Method Post -ContentType "application/json; charset=utf-8" -Body $body
```

### 查看 mcp-server 日志

应该看到详细的错误信息：

```
=== 收到 schema.get 请求 ===
请求体: SchemaRequest(table=customers)
表名: customers
开始查询表结构: customers
执行验证 SQL: SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = UPPER(?), 参数: customers
[这里会出现具体错误]
```

## 可能的错误原因

### 1. H2 数据库未初始化
**症状**：`schema.sql` 或 `data.sql` 未执行
**解决**：检查 mcp-server 启动日志，看是否有 "Executed SQL script"

### 2. Lombok 注解未生成代码
**症状**：SchemaRequest.getTable() 方法不存在
**解决**：检查 mcp-server/pom.xml 是否有 maven-compiler-plugin 配置

### 3. JDBC 连接失败
**症状**：无法连接到 H2 数据库
**解决**：检查 application.yml 的数据源配置

### 4. JSON 反序列化失败
**症状**：@RequestBody 绑定失败
**解决**：检查请求 Content-Type 和 DTO 结构

## 已添加的日志点

### McpController.java
- `log.info("=== 收到 schema.get 请求 ===")`
- `log.info("请求体: {}", request)`
- `log.info("表名: {}", request.getTable())`
- `log.error("=== schema.get 处理失败 ===", e)`

### DatabaseService.java
- `log.info("开始查询表结构: {}", tableName)`
- `log.info("执行验证 SQL: {}, 参数: {}", checkTableSql, tableName)`
- `log.info("表存在检查结果: {}", count)`
- `log.error("查询表结构失败: tableName={}", tableName, e)`

## 下一步

1. **重启 mcp-server**（必须重启才能加载新日志代码）
2. **触发错误**（使用 Web UI 或 PowerShell）
3. **查看 mcp-server 控制台输出**
4. **提供完整错误堆栈**

请将 mcp-server 的完整错误日志发给我，我会根据具体错误进行修复。
