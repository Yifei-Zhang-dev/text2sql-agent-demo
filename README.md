# Text-to-SQL Agent Demo

> ⚠️ 免责声明：本项目为学习演示项目，仅供学习参考。不适用于生产环境，不提供任何维护或支持。

基于 **Spring AI Alibaba** 的 Text-to-SQL 智能问答系统。用户输入自然语言问题，系统自动生成 SQL 查询并以图表（柱状图、饼图、折线图、表格等）形式展示结果。

项目提供两种运行模式：

- **单 Agent 模式** — 由一个 Agent 完成从理解问题到生成可视化的全部工作，适合快速查询。
- **Graph 模式** — 由 6 个专职节点（路由 → Schema 检索 → SQL 生成 → SQL 校验 → 可视化渲染）协作完成，查询更准确，可处理复杂的多表关联和聚合统计。这是项目的核心亮点，展示了 Spring AI Alibaba Graph 的多 Agent 编排能力。

## 项目结构

```
spring-ai-demo/
├── demo-saia/          # 主应用（Spring AI Alibaba + Graph）  端口 8082
├── mcp-server/         # MCP Server（数据库操作服务）           端口 8083
├── demo-sai/           # 旧模块（Spring AI OpenAI），已不再使用
└── pom.xml             # 父 POM
```

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 运行环境 | JDK | 17 |
| 框架 | Spring Boot | 3.5.9 |
| AI 框架 | Spring AI | 1.0.0 GA |
| AI 框架 | Spring AI Alibaba | 1.0.0.2 |
| 大模型 | 通义千问 (Qwen-Plus) | — |
| 数据库 | H2（内存数据库） | — |
| 前端 | HTML / CSS / JS + ECharts 5.4 | — |
| 构建工具 | Maven | — |

## 快速开始

### 前置要求

- JDK 17+
- Maven 3.8+
- 通义千问 API Key（[申请地址](https://dashscope.console.aliyun.com/)）

### 1. 配置 API Key

编辑 `demo-saia/src/main/resources/application.yml`，设置环境变量或直接替换：

```yaml
spring:
  ai:
    dashscope:
      api-key: ${QWEN_API_KEY}    # 替换为你的 API Key，或设置环境变量 QWEN_API_KEY
```

也可以通过环境变量配置：

```bash
# Linux / macOS
export QWEN_API_KEY=sk-xxxxxxxxxxxxxxxx

# Windows PowerShell
$env:QWEN_API_KEY="sk-xxxxxxxxxxxxxxxx"
```

### 2. 启动 MCP Server（必须先启动）

```bash
cd mcp-server
mvn spring-boot:run
```

启动成功后可访问：
- H2 控制台：http://localhost:8083/h2-console （JDBC URL: `jdbc:h2:mem:mcpdb`，用户名 `sa`，密码为空）
- 工具列表：http://localhost:8083/mcp/tools

### 3. 启动主应用

新开一个终端：

```bash
cd demo-saia
mvn spring-boot:run
```

### 4. 访问页面

打开浏览器访问：

| 模式 | URL | 说明 |
|------|-----|------|
| 单 Agent | http://localhost:8082/?mode=single | 默认模式，单一 Agent 处理 |
| Graph | http://localhost:8082/?mode=graph | 多节点协作，支持复杂查询 |

页面上提供了示例问题标签，可以直接点击体验。

## Graph 模式节点说明

```
用户问题 → RouterNode（路由分类）
               ├─ 简单查询 → SimpleSqlGeneratorNode → SqlValidatorNode → RendererNode → 结果
               └─ 复杂查询 → SchemaRetrievalNode → ComplexSqlGeneratorNode → SqlValidatorNode → RendererNode → 结果
```

| 节点 | 职责 |
|------|------|
| RouterNode | 将问题分类为「简单」或「复杂」 |
| SchemaRetrievalNode | 通过 MCP 获取表结构信息（仅复杂查询） |
| SimpleSqlGeneratorNode | 直接生成简单查询 SQL |
| ComplexSqlGeneratorNode | 基于 Schema 生成多表关联 SQL |
| SqlValidatorNode | 校验 SQL 语法和安全性 |
| RendererNode | 生成 JavaScript 可视化代码（ECharts） |

## 数据库说明

MCP Server 使用 H2 内存数据库，启动时自动初始化 3 张表和示例数据：

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| customers | 客户信息（110 条） | name, email, phone, city |
| orders | 订单记录 | customer_id, order_date, total_amount, status |
| order_items | 订单明细 | order_id, product_name, quantity, unit_price |

> 注意：没有独立的产品表，商品信息存储在 `order_items.product_name` 中。

## 注意事项

- **启动顺序**：必须先启动 mcp-server（8083），再启动 demo-saia（8082），否则主应用无法调用数据库工具。
- **端口占用**：确保 8082 和 8083 端口未被占用。
- **内存数据库**：H2 数据在服务重启后会重置，每次启动都是全新的示例数据。
- **模型选择**：默认使用 `qwen-plus`，可在 `application.yml` 中修改为其他通义千问模型。
