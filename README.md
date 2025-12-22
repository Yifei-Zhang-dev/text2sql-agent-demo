# Web UI + Swagger UI

## 新增功能

### 1. 简单 Web 界面
- 访问地址：http://localhost:8081/
- 功能：
  - 输入自然语言问题
  - 点击按钮调用 `/agent/text2sql` 接口
  - 显示生成的 SQL、查询结果、解释和错误信息
  - 提供示例问题快速测试
  - 支持 Ctrl+Enter 快捷键提交
- 技术栈：纯 HTML + CSS + Fetch API（无需 npm）

### 2. Swagger UI 文档
- 访问地址：http://localhost:8081/swagger-ui.html
- 功能：
  - 自动生成 API 文档
  - 在线测试所有接口
  - 查看请求/响应结构
- OpenAPI 文档：http://localhost:8081/v3/api-docs

## 文件变更

### 新增文件
1. `demo-sai/src/main/resources/static/index.html` - Web 界面

### 修改文件
1. `demo-sai/pom.xml`
   - 添加 `springdoc-openapi-starter-webmvc-ui` 依赖（版本 2.7.0）

2. `demo-sai/src/main/resources/application.yml`
   - 配置 Swagger UI 路径

3. `demo-sai/src/main/java/com/example/demo/controller/AgentController.java`
   - 添加 `@Tag` 和 `@Operation` 注解

4. `demo-sai/src/main/java/com/example/demo/controller/DebugController.java`
   - 添加 `@Tag` 和 `@Operation` 注解

## 使用说明

### 启动步骤

**1. 启动 MCP Server (端口 8083)**
```powershell
cd mcp-server
..\mvnw.cmd spring-boot:run
```

**2. 启动 demo-sai (端口 8081)**
```powershell
# 设置 API Key
$env:QWEN_API_KEY="your-api-key-here"

cd demo-sai
..\mvnw.cmd spring-boot:run
```

### 测试 Web 界面

1. 打开浏览器访问：http://localhost:8081/
2. 输入问题，例如：
   - 查询所有客户的姓名和邮箱
   - 统计每个客户的订单总数
   - 查询总金额最高的订单
   - 列出所有待发货的订单
3. 点击"提交查询"按钮
4. 查看结果：
   - **生成的 SQL**：LLM 生成的 SQL 语句
   - **查询结果**：SQL 执行结果（JSON 格式）
   - **结果解释**：LLM 对结果的中文解释
   - **错误信息**：如有错误会显示

### 测试 Swagger UI

1. 打开浏览器访问：http://localhost:8081/swagger-ui.html
2. 展开 "Agent API" 或 "Debug API"
3. 点击 "Try it out" 测试接口
4. 填写请求参数并点击 "Execute"

### 快捷链接

- 主页：http://localhost:8081/
- Swagger UI：http://localhost:8081/swagger-ui.html
- API 文档：http://localhost:8081/v3/api-docs
- 健康检查：http://localhost:8081/actuator/health
- 配置调试：http://localhost:8081/debug/full-config

## 技术细节

### Swagger UI 配置
```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
```

### OpenAPI 注解示例
```java
@Tag(name = "Agent API", description = "智能代理接口，支持自然语言转 SQL 查询")
@RestController
@RequestMapping("/agent")
public class AgentController {

    @Operation(summary = "自然语言转 SQL", description = "输入自然语言问题，自动生成 SQL 并执行，返回查询结果和解释")
    @PostMapping("/text2sql")
    public Text2SqlResponse text2Sql(@RequestBody Text2SqlRequest request) {
        // ...
    }
}
```

### Web 界面特性
- 响应式设计，支持移动端
- 渐变背景 + 卡片布局
- 加载动画显示请求状态
- 错误信息红色高亮
- 代码块使用等宽字体
- 示例问题快速填充
- Ctrl+Enter 快捷提交
