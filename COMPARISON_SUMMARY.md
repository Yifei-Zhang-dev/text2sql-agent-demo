# Spring AI vs Spring AI Alibaba - 技术选型对比总结（PPT专用简版）

## 一、版本信息

| 项目 | 版本 | 发布日期 | 状态 | 官方来源 |
|------|------|---------|------|---------|
| **Spring AI** | **1.0.0 GA** | 2025-05-20 | ✅ 生产可用 | [Spring Blog](https://spring.io/blog/2025/05/20/spring-ai-1-0-GA-released/) |
| **Spring AI Alibaba** | **1.0.0.2 GA** | 2025-06 | ✅ 生产可用 | [Alibaba Blog](https://www.alibabacloud.com/blog/spring-ai-alibaba-1-0-ga-officially-released-marking-the-advent-of-a-new-era-in-java-agent-development_602299) |
| **Spring Boot** | **3.5.9** | 2025 | ✅ 最新稳定版 | [Spring Boot Releases](https://github.com/spring-projects/spring-boot/releases) |

**关键点**：
- ✅ 两个 demo 都使用 **Spring AI 1.0.0 GA** 作为核心（无版本混用）
- ✅ Spring AI Alibaba 基于 Spring AI 1.0.0 构建
- ✅ 所有依赖都是 GA 版本（无 Milestone、RC、SNAPSHOT）

---

## 二、依赖验证（证明无版本混用）

### demo-sai (Spring AI)
```
所有 org.springframework.ai:* 都是 1.0.0
├── spring-ai-starter-model-openai:1.0.0
├── spring-ai-openai:1.0.0
├── spring-ai-model:1.0.0
├── spring-ai-commons:1.0.0
└── spring-ai-client-chat:1.0.0
```

### demo-saia (Spring AI Alibaba)
```
所有 org.springframework.ai:* 都是 1.0.0
所有 com.alibaba.cloud.ai:* 都是 1.0.0.2

├── spring-ai-alibaba-starter-dashscope:1.0.0.2
│   └── spring-ai-alibaba-core:1.0.0.2
│       ├── org.springframework.ai:spring-ai-commons:1.0.0
│       ├── org.springframework.ai:spring-ai-model:1.0.0
│       └── org.springframework.ai:spring-ai-client-chat:1.0.0
```

**验证命令**（可在 PPT 中展示）：
```bash
# demo-sai
cd demo-sai && mvn dependency:tree -Dincludes=org.springframework.ai

# demo-saia
cd demo-saia && mvn dependency:tree -Dincludes=org.springframework.ai,com.alibaba.cloud.ai
```

---

## 三、配置对比

### Maven 依赖

| 模块 | Spring AI | Spring AI Alibaba |
|------|-----------|-------------------|
| **Starter** | `spring-ai-starter-model-openai` | `spring-ai-alibaba-starter-dashscope` |
| **BOM 版本** | 1.0.0 GA | 1.0.0.2 GA |

### application.yml

| 配置项 | Spring AI | Spring AI Alibaba |
|--------|-----------|-------------------|
| **配置前缀** | `spring.ai.openai.*` | `spring.ai.dashscope.*` |
| **Base URL** | 需要配置（OpenAI兼容模式） | **无需配置**（原生SDK） |
| **API Key** | `QWEN_API_KEY` | `QWEN_API_KEY` |
| **模型** | qwen-turbo | qwen-plus |
| **配置复杂度** | 略高（需手动设置 base-url） | **更简洁** |

---

## 四、代码对比

### 统一结构（100% 一致）

```
controller/   - AgentController、DiagnosticController
service/      - Text2SqlService、McpToolService
config/       - ChatClientConfig、McpFunctionConfig
dto/          - Text2SqlRequest、Text2SqlResponse
```

### API 调用（100% 一致）

```java
// ✅ 两个 demo 完全相同
ChatResponse chatResponse = chatClient.prompt()
    .system(SYSTEM_PROMPT)
    .user(request.getQuestion())
    .toolNames("schemaGet", "sqlRun")  // Spring AI 1.0.0 GA 标准 API
    .call()
    .chatResponse();

String llmAnswer = chatResponse.getResult().getOutput().getText();
```

### 唯一差异（仅日志）

| 文件 | Spring AI | Spring AI Alibaba |
|------|-----------|-------------------|
| 日志标识 | `(Spring AI)` | `(Spring AI Alibaba)` |
| **业务代码** | **完全相同** | **完全相同** |

---

## 五、功能对比

| 功能 | Spring AI | Spring AI Alibaba | 结论 |
|------|-----------|-------------------|------|
| **Tool Calling** | ✅ | ✅ | 完全对等 |
| **Bean-based Tools** | ✅ | ✅ | API 一致 |
| **ChatClient** | ✅ | ✅ | 完全相同 |
| **Retry 机制** | ✅ | ✅（继承） | 一致 |
| **跨 Provider** | ✅ 多Provider | ⚠️ DashScope only | Spring AI 优势 |
| **配置简洁性** | ⚠️ 需base-url | ✅ 更简洁 | Alibaba 优势 |

---

## 六、选型建议

### 选择 Spring AI（官方版）✅

- ✅ 需要跨 LLM Provider（OpenAI/Azure/Claude 等）
- ✅ 未来可能迁移到其他云平台
- ✅ 团队熟悉 Spring 官方生态
- ✅ 希望获得国际社区支持
- ✅ 项目需要长期维护，追求稳定性

### 选择 Spring AI Alibaba ✅

- ✅ 已使用阿里云 DashScope（通义千问）
- ✅ 深度绑定阿里云生态
- ✅ 需要原生 DashScope 功能
- ✅ 团队主要面向国内用户
- ✅ 希望简化配置（无需 base-url）

---

## 七、迁移成本

### 从 Spring AI → Spring AI Alibaba

**成本**: **极低**

**改动**:
1. `pom.xml`: 替换 Starter（1 处）
2. `application.yml`: 修改配置前缀 + 删除 base-url（1 处）
3. 日志标识（可选）

**业务代码**: **0 改动**

### 从 Spring AI Alibaba → Spring AI

**成本**: **极低**

**改动**:
1. `pom.xml`: 替换 Starter（1 处）
2. `application.yml`: 修改配置前缀 + 添加 base-url（1 处）
3. 日志标识（可选）

**业务代码**: **0 改动**

---

## 八、风险规避

### 已解决的问题 ✅

| 问题 | 旧版本 | 当前版本 | 解决方案 |
|------|--------|---------|---------|
| **版本混用** | M5 + 1.0.0 混用 | ❌ | 统一使用 1.0.0 GA |
| **API 过时** | `.functions()` | ❌ | 使用 `.toolNames()` |
| **options null** | 运行时报错 | ❌ | 通过 application.yml 注入 |

### 当前最佳实践 ✅

1. ✅ 使用 GA 版本（禁止 M/RC/SNAPSHOT）
2. ✅ 使用 Spring AI 1.0.0 GA 推荐 API
3. ✅ 通过 application.yml 配置 options
4. ✅ 统一依赖版本（dependency:tree 验证）

---

## 九、测试数据

### MCP Server 数据集

- **10 个客户**（张三、李四、王五...）
- **15 个订单**（总金额约 148,440 元）
- **30+ 个订单项**（MacBook、iPhone、iPad、AirPods 等）

### 测试用例（PPT 可演示）

1. ✅ 基础查询：查询所有客户姓名和邮箱
2. ✅ 聚合查询：统计每个客户的订单总数（JOIN + GROUP BY）
3. ✅ 聚合计算：计算所有订单的总金额（SUM）
4. ✅ 排序查询：查询金额最高的订单（ORDER BY + LIMIT + JOIN）
5. ✅ 条件查询：查询已完成订单的客户（WHERE + DISTINCT）
6. ✅ 复杂查询：查询购买过 MacBook 的客户（3表 JOIN + LIKE）

详细脚本参见：[TEST_CASES.md](./TEST_CASES.md)

---

## 十、PPT 演示建议

### 关键截图点

1. **依赖树对比**：展示两个 demo 的 dependency:tree 输出（证明版本统一）
2. **配置对比**：并排展示 application.yml（突出 base-url 差异）
3. **代码对比**：展示 Text2SqlService.java（强调业务代码一致）
4. **日志对比**：展示控制台输出（schemaGet → sqlRun 调用链）
5. **响应对比**：展示 JSON 响应（executedSql、llmAnswer、status）

### 演示流程

1. **启动演示**（3 个服务：mcp-server、demo-sai、demo-saia）
2. **健康检查**（curl /actuator/health）
3. **简单查询**（用例 1：查询所有客户）
4. **复杂查询**（用例 6：3 表 JOIN）
5. **对比总结**（并排展示两个 demo 的响应）

---

## 十一、最终结论

### 核心发现

1. **代码兼容性**: 业务代码 **100% 一致**
2. **API 统一性**: 都使用 Spring AI 1.0.0 GA 标准 API
3. **功能对等性**: Tool Calling 等核心功能**完全对等**
4. **迁移便利性**: 切换成本**极低**

### 推荐策略

| 场景 | 推荐方案 | 核心理由 |
|------|---------|---------|
| **新项目** | Spring AI | 灵活性高，未来可平滑迁移 |
| **阿里云重度用户** | Spring AI Alibaba | 配置简单，原生集成 |
| **多云部署** | Spring AI | 跨 Provider 能力强 |
| **快速验证** | 任意选择 | 迁移成本低，无需过度纠结 |

### 技术债务风险

⚠️ **避免版本混用**: 确保 `org.springframework.ai:*` 只有一个版本  
⚠️ **避免过时 API**: 使用 `.toolNames()` 和 `.getText()`  
⚠️ **配置 options**: 通过 `application.yml` 设置，避免运行时为 null

---

**文档版本**: 2.0  
**更新日期**: 2025-12-28  
**技术栈**: Spring AI 1.0.0 GA + Spring AI Alibaba 1.0.0.2 GA + Spring Boot 3.5.9  
**作者**: Claude Code (Powered by Claude Sonnet 4.5)
