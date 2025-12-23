# Demo SAIA 交付验收清单

## 一、构建验证

### 1.1 依赖解析成功

```bash
cd demo-saia
mvn clean compile -DskipTests
```

**预期**：`BUILD SUCCESS`

### 1.2 完整构建（包含根项目）

```bash
cd D:\projects\spring-ai-demo
mvn clean compile -DskipTests
```

**预期**：
```
[INFO] Spring AI Demo ..................................... SUCCESS
[INFO] Demo SAI (Spring AI) ............................... SUCCESS
[INFO] Demo SAIA (Spring AI Alibaba) ...................... SUCCESS
[INFO] MCP Server ......................................... SUCCESS
```

## 二、启动验证（按顺序）

### 2.1 环境变量

```powershell
$env:QWEN_API_KEY="sk-xxx"  # 替换为真实 Key
```

### 2.2 MCP Server（必须先启动）

```bash
cd mcp-server
mvn spring-boot:run
```

**验证点**：
- [ ] 启动日志无报错
- [ ] 浏览器打开 http://localhost:8083/mcp/tools 返回 JSON
- [ ] JSON 包含 `schema.get` 和 `sql.run`

### 2.3 Demo SAIA

```bash
cd demo-saia
mvn spring-boot:run
```

**验证点**：
- [ ] 启动日志显示 `Started DemoSaiaApplication`
- [ ] 端口 8082 监听成功
- [ ] 无 `Caused by` 异常栈

## 三、功能验证

### 3.1 健康检查

访问：http://localhost:8082/actuator/health

**预期**：
```json
{
  "status": "UP"
}
```

### 3.2 Web UI 可访问

访问：http://localhost:8082/

**预期**：
- [ ] 页面标题：`Spring AI Alibaba Demo - Text2SQL`
- [ ] 副标题包含 `Powered by DashScope`
- [ ] 输入框可编辑
- [ ] 4 个示例标签可点击

### 3.3 完整 Text-to-SQL 流程

**输入问题**：`查询所有客户的姓名和邮箱`

**预期结果**：
1. **生成的 SQL**：
   ```sql
   SELECT name, email FROM customers
   ```
   或类似的正确 SELECT 语句

2. **查询结果**：
   - 显示 `查询结果 (共 X 行)`
   - 列名包含 `name, email`
   - 至少一行数据（如 `[张三, zhang@example.com]`）

3. **结果解释**：
   - 中文说明（1-2 句话）
   - 类似：`查询了所有客户的姓名和邮箱，共 X 条记录。`

### 3.4 日志验证

**必须出现的日志**：
```
========== Text-to-SQL 请求开始 (Spring AI Alibaba) ==========
用户问题: 查询所有客户的姓名和邮箱
正在调用 LLM (Spring AI Alibaba)...
=== 调用 MCP Tool: schema.get ===
请求参数: table=customers
=== 调用 MCP Tool: sql.run ===
SQL: SELECT ...
LLM 最终响应: ...
========== Text-to-SQL 请求完成 ==========
```

### 3.5 多轮测试

分别输入以下问题，确保每次都能正常返回：

1. `统计每个客户的订单总数`
2. `查询总金额最高的订单`
3. `列出所有待发货的订单`

## 四、API 验证（可选）

### 4.1 POST /agent/text2sql

```bash
curl -X POST http://localhost:8082/agent/text2sql \
  -H "Content-Type: application/json" \
  -d "{\"question\": \"查询所有客户的姓名和邮箱\"}"
```

**预期**：返回 JSON 包含 `sql`、`result`、`explanation`

### 4.2 Swagger UI

访问：http://localhost:8082/swagger-ui.html

**验证点**：
- [ ] `Agent API (Alibaba)` 分组存在
- [ ] `/agent/text2sql` 接口可见
- [ ] 可执行测试请求

## 五、对比验证（与 demo-sai）

### 5.1 同时启动两个服务

- **Demo SAI**: http://localhost:8081/
- **Demo SAIA**: http://localhost:8082/

### 5.2 输入相同问题

在两个 UI 中分别输入：`查询所有客户的姓名和邮箱`

**预期**：
- [ ] 两者生成的 SQL 结构相似（可能字段顺序不同）
- [ ] 查询结果数据一致
- [ ] 中文解释逻辑相似

## 六、代码质量检查

### 6.1 无破坏性改动

**验证**：
```bash
git status
```

**不应出现**：
- `demo-sai/` 目录下的任何修改
- `mcp-server/` 目录下的任何修改（除 `.idea/compiler.xml`）
- 根 `pom.xml` 中 Spring AI 版本的修改

**允许出现**：
- `pom.xml` 中新增 `<module>demo-saia</module>`
- 新增 `demo-saia/` 整个目录
- 新增 `COMPARISON.md`

### 6.2 依赖版本稳定性

检查 `demo-saia/pom.xml`：

**必须满足**：
- [ ] `spring-ai-alibaba-bom` 版本为 `1.0.0.2`（GA，非 M/RC/SNAPSHOT）
- [ ] 所有依赖能从 Maven Central 下载
- [ ] 无 `<version>` 标签直接写在 `spring-ai-alibaba-starter-dashscope`

## 七、文档完整性

### 7.1 README.md 存在

文件：`demo-saia/README.md`

**必须包含**：
- [ ] 启动步骤
- [ ] 环境变量配置
- [ ] 验收测试方法
- [ ] 依赖版本信息（含验证来源）

### 7.2 COMPARISON.md 存在

文件：`D:\projects\spring-ai-demo\COMPARISON.md`

**必须包含**：
- [ ] PPT 对比表格
- [ ] 代码差异对比
- [ ] 技术选型建议

## 八、最终交付物清单

```
D:\projects\spring-ai-demo\
├── pom.xml（已修改：新增 demo-saia 模块）
├── COMPARISON.md（新增：技术对比文档）
├── demo-sai/（未修改，保持原样）
├── demo-saia/（新增：Spring AI Alibaba 版本）
│   ├── pom.xml
│   ├── README.md
│   ├── DELIVERY-CHECKLIST.md（本文档）
│   └── src/
│       └── main/
│           ├── java/com/example/demosaia/
│           │   ├── DemoSaiaApplication.java
│           │   ├── config/
│           │   │   ├── McpClientConfig.java
│           │   │   └── McpFunctionConfig.java
│           │   ├── controller/
│           │   │   └── AgentController.java
│           │   ├── dto/
│           │   │   ├── Text2SqlRequest.java
│           │   │   └── Text2SqlResponse.java
│           │   └── service/
│           │       └── McpToolService.java
│           └── resources/
│               ├── application.yml
│               └── static/
│                   └── index.html
└── mcp-server/（未修改，保持原样）
```

## 九、验收通过标准

全部打勾即为验收通过：

- [ ] 所有构建验证通过
- [ ] MCP Server + Demo SAIA 同时运行无报错
- [ ] Web UI 功能完整可用
- [ ] 至少成功执行 1 次完整 Text-to-SQL 流程
- [ ] 日志显示正确调用 `schema.get` + `sql.run`
- [ ] 无破坏 demo-sai 原有功能
- [ ] 文档齐全（README.md + COMPARISON.md）

---

**验收人**：___________
**日期**：___________
**签字**：___________
