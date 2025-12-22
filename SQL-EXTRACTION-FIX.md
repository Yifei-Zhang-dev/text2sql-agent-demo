# SQL 提取功能修复

## 问题
Web UI 显示 "（未检测到 SQL）"，因为 LLM 的最终响应只包含解释文字，不包含 SQL 语句。

## 根本原因
Spring AI 的 Function Calling 机制中，SQL 是通过函数调用传递给 `sqlRun` 工具的，不会出现在 LLM 的最终文本响应中。

## 解决方案

### 1. 在 McpToolService 中使用 ThreadLocal 跟踪 SQL

```java
private static final ThreadLocal<String> lastExecutedSql = new ThreadLocal<>();

public String runSql(String sql) {
    // 保存 SQL 到 ThreadLocal
    lastExecutedSql.set(sql);
    // ...执行 SQL
}

public static String getLastExecutedSql() {
    return lastExecutedSql.get();
}
```

### 2. 在 AgentController 中获取 SQL

```java
// 从 McpToolService 获取最后执行的 SQL
String executedSql = McpToolService.getLastExecutedSql();

return new Text2SqlResponse(
        executedSql != null ? executedSql : "（未检测到 SQL）",
        response,
        "查询完成"
);
```

## 测试步骤

```powershell
# 1. 停止 demo-sai (Ctrl+C)

# 2. 重启 demo-sai
$env:QWEN_API_KEY="your-api-key"
cd D:\projects\spring-ai-demo\demo-sai
..\mvnw.cmd spring-boot:run

# 3. 测试 Web UI
# 访问 http://localhost:8081/
# 输入："查询所有客户的姓名和邮箱"
# 点击提交
```

## 预期结果

Web UI 应该显示：
- **生成的 SQL**: `SELECT NAME, EMAIL FROM customers`
- **查询结果**: LLM 的解释文字
- **结果解释**: "查询完成"

## 文件修改

1. `demo-sai/src/main/java/com/example/demo/service/McpToolService.java`
   - 添加 ThreadLocal 跟踪 SQL
   - 在 runSql 方法中保存 SQL
   - 添加 getLastExecutedSql 静态方法

2. `demo-sai/src/main/java/com/example/demo/controller/AgentController.java`
   - 调用 McpToolService.getLastExecutedSql() 获取 SQL
   - 简化代码，移除正则表达式提取逻辑
