package com.example.demosaia.service;

import com.example.demosaia.dto.ScriptResponse;
import com.example.demosaia.dto.Text2SqlRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text2SQL 业务逻辑服务
 * - 核心流程：接收自然语言问题 → 调用 LLM 生成 JavaScript 脚本 → 返回脚本代码
 * - 使用 Spring AI 1.0.0 GA API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Text2SqlService {

    private final ChatClient chatClient;

    /**
     * System Prompt（指导 LLM 生成 JavaScript 脚本）
     */
    private static final String SYSTEM_PROMPT = """
            你是一个数据可视化助手。请按以下步骤操作：

            步骤1: 分析用户问题，判断最适合的组件类型
            - 如果是"列表"、"详情"、"所有" → 使用 Table
            - 如果是"分布"、"占比"、"比例" → 使用 PieChart
            - 如果是"总数"、"当前"、"数量" → 使用 DataPoint

            步骤2: 调用 schemaGet 工具获取表结构

            步骤3: 生成JavaScript异步函数（重要！必须用```javascript代码块包裹）

            函数格式：
            ```javascript
            async function generateData(mcpClient) {
                // 1. 获取表结构（如果需要）
                const schema = await mcpClient.getSchema('表名');

                // 2. 生成并执行SQL
                const sql = "SELECT ... FROM ... WHERE ...";
                const result = await mcpClient.executeSql(sql);

                // 3. 转换为组件数据格式并返回
                return {
                    componentType: 'Table',  // 或 'PieChart'、'DataPoint'
                    propertyData: { ... }    // 根据组件类型转换数据
                };
            }
            ```

            组件数据格式规范：
            - Table: { rows: [{key: 1, field1: value1, ...}], total: N }
              (rows中每个对象必须包含key字段，从1开始自增)
            - PieChart: [{name: '名称', value: 数值}, ...]
            - DataPoint: { value: 数值 }

            约束规则：
            - 代码必须放在```javascript代码块中
            - 必须使用async/await语法
            - Table的rows中必须有key字段（在JavaScript代码中手动添加，不要在SQL中使用AS key）
            - 字段名要与SQL查询的列名对应
            - 先调用schemaGet再生成SQL
            - SQL只支持SELECT语句
            - SQL中禁止使用保留字作为别名（如key、order、table等），直接查询原始列名即可

            步骤4: 用1-2句话简短解释查询结果

            可用表：
            - customers：客户信息（ID, NAME, EMAIL, ADDRESS）
            - orders：订单记录（ID, CUSTOMER_ID, ORDER_DATE, TOTAL_AMOUNT）
            - order_items：订单明细（ID, ORDER_ID, PRODUCT_NAME, QUANTITY, PRICE）
            """;

    /**
     * 执行 Text-to-SQL 流程
     *
     * @param request 用户请求（包含自然语言问题）
     * @return ScriptResponse（包含 JavaScript 脚本和解释）
     */
    public ScriptResponse executeText2Sql(Text2SqlRequest request) {
        log.info("收到查询请求: {}", request.getQuestion());

        try {
            // 1. 调用 ChatClient（只使用 schemaGet 工具）
            ChatResponse chatResponse = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(request.getQuestion())
                    .toolNames("schemaGet")  // 只提供 schemaGet，不提供 sqlRun
                    .call()
                    .chatResponse();

            // 2. 获取 LLM 的完整回复
            String llmResponse = chatResponse.getResult().getOutput().getText();
            log.info("LLM完整回复: {}", llmResponse);

            // 3. 提取 JavaScript 脚本代码
            String scriptCode = extractScriptCode(llmResponse);
            log.info("提取到的脚本代码: {}", scriptCode);

            // 4. 提取中文解释
            String explanation = extractExplanation(llmResponse);
            log.info("提取到的解释: {}", explanation);

            // 5. 返回 ScriptResponse
            return ScriptResponse.builder()
                    .scriptCode(scriptCode)
                    .explanation(explanation)
                    .build();

        } catch (Exception e) {
            log.error("执行查询失败", e);
            // 返回错误信息
            return ScriptResponse.builder()
                    .scriptCode("// 生成脚本失败")
                    .explanation("查询失败：" + e.getMessage())
                    .build();
        }
    }

    /**
     * 从 LLM 回复中提取 JavaScript 代码
     */
    private String extractScriptCode(String llmResponse) {
        // 使用正则表达式提取 ```javascript 和 ``` 之间的内容
        Pattern pattern = Pattern.compile("```javascript\\s*([\\s\\S]*?)```");
        Matcher matcher = pattern.matcher(llmResponse);

        if (matcher.find()) {
            String code = matcher.group(1).trim();
            log.info("成功提取脚本代码，长度: {}", code.length());
            return code;
        }

        // 如果没有代码块标记，尝试查找 async function
        if (llmResponse.contains("async function")) {
            log.warn("未找到代码块标记，但发现async function，返回全部内容");
            return llmResponse;
        }

        // 兜底：返回整个回复
        log.warn("未找到脚本代码，返回整个LLM回复");
        return llmResponse;
    }

    /**
     * 从 LLM 回复中提取中文解释
     */
    private String extractExplanation(String llmResponse) {
        // 去除代码块后的内容
        String text = llmResponse.replaceAll("```javascript[\\s\\S]*?```", "");
        text = text.trim();

        // 如果为空，返回默认值
        if (text.isEmpty()) {
            return "已生成查询脚本";
        }

        return text;
    }

    /**
     * 通用对话接口（不使用工具）
     *
     * @param message 用户消息
     * @return LLM 响应
     */
    public String chat(String message) {
        log.info("收到对话请求: {}", message);

        try {
            return chatClient.prompt()
                    .user(message)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("对话失败", e);
            return "对话失败: " + e.getMessage();
        }
    }
}
