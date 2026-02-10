package com.example.demosaia.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.example.demosaia.graph.state.Text2SqlState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renderer Node - 脚本渲染节点
 * 职责：根据问题和 SQL 生成 JavaScript 脚本和选择组件类型
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RendererNode implements Function<OverAllState, Map<String, Object>> {

    private final ChatClient chatClient;

    private static final String RENDERER_PROMPT = """
            你是数据可视化专家。根据用户问题和已生成的 SQL，先生成中文说明，再生成 JavaScript 脚本。

            【已生成的 SQL】
            {sql}

            【用户问题】
            {question}

            【任务】
            1. 首先，用中文写一段简洁的说明（1-2句话），放在代码块之前。说明要求：
               - 用通俗易懂的语言描述查询做了什么、结果用什么方式展示
               - 禁止使用任何 Markdown 标记（不要用反引号、```、**、# 等）
               - 禁止提及具体的表名、字段名、SQL 语法、LIMIT 等技术细节
               - 示例：「查询所有客户的姓名和城市，以表格形式展示，最多显示200条。」
            2. 然后，生成一个 async function generateData(mcpClient) 函数。
            函数必须调用 mcpClient.executeSql(sql) 执行 SQL，然后返回 { componentType, propertyData }。

            【mcpClient.executeSql 返回值说明】
            返回一个增强数组，同时支持：
            - result.rows（对象数组），result.columns（列名数组），result.rowCount（行数）
            - result.map()、result[0] 等数组操作
            - 每行对象同时有原始列名、小写列名、大写列名（如 row.NAME、row.name）

            【组件类型及数据格式】

            1. Table（列表查询）：
            ```javascript
            async function generateData(mcpClient) {
                const sql = "已有的SQL";
                const result = await mcpClient.executeSql(sql);
                const rows = result.rows.map((row, index) => ({ key: index + 1, ...row }));
                return {
                    componentType: 'Table',
                    propertyData: {
                        rows: rows
                    }
                };
            }
            ```

            2. Table（聚合查询，如"每个客户的订单数"）：
            ```javascript
            async function generateData(mcpClient) {
                const sql = "已有的SQL";
                const result = await mcpClient.executeSql(sql);
                const rows = result.rows.map((row, index) => ({ key: index + 1, ...row }));
                return {
                    componentType: 'Table',
                    propertyData: {
                        rows: rows
                    }
                };
            }
            ```

            3. DataPoint（单值统计，如"客户总数"）：
            ```javascript
            async function generateData(mcpClient) {
                const sql = "SELECT COUNT(*) as TOTAL FROM customers";
                const result = await mcpClient.executeSql(sql);
                const count = result.rows[0].TOTAL || result.rows[0].total || 0;
                return {
                    componentType: 'DataPoint',
                    propertyData: { value: count, label: '客户总数' }
                };
            }
            ```

            4. LineChart（趋势，如"每月订单量趋势"）：
            ```javascript
            async function generateData(mcpClient) {
                const sql = "已有的SQL";
                const result = await mcpClient.executeSql(sql);
                const chartData = result.rows.map(row => ({
                    name: (row.ORDER_MONTH || row.order_month) + '月',
                    value: row.ORDER_COUNT || row.order_count || 0
                }));
                return {
                    componentType: 'LineChart',
                    propertyData: chartData
                };
            }
            ```

            5. BarChart（对比，如"各城市客户数"）：
            ```javascript
            async function generateData(mcpClient) {
                const sql = "已有的SQL";
                const result = await mcpClient.executeSql(sql);
                const chartData = result.rows.map(row => ({
                    name: row.CITY || row.city || '未知',
                    value: row.CUSTOMER_COUNT || row.customer_count || 0
                }));
                return {
                    componentType: 'BarChart',
                    propertyData: chartData
                };
            }
            ```

            6. PieChart（分布/占比，如"订单状态分布"）：
            ```javascript
            async function generateData(mcpClient) {
                const sql = "已有的SQL";
                const result = await mcpClient.executeSql(sql);
                const chartData = result.rows.map(row => ({
                    name: row.STATUS || row.status,
                    value: row.COUNT || row.count || 0
                }));
                return {
                    componentType: 'PieChart',
                    propertyData: chartData
                };
            }
            ```

            【组件选择指南】
            - 列表查询（列出所有客户、显示订单等）→ Table
            - 聚合查询（每个客户的订单数等）→ Table
            - 单值统计（总数、总金额、平均值等）→ DataPoint
            - 时间趋势（每月、每年的变化）→ LineChart
            - 分类对比/排行（各城市、各产品等）→ BarChart
            - 占比分布（状态分布、类别比例）→ PieChart

            【严格要求】
            - 必须用 ```javascript 代码块包裹代码
            - 必须定义 async function generateData(mcpClient)
            - SQL 字符串直接写死在脚本中（使用已生成的 SQL）
            - 使用 await mcpClient.executeSql(sql) 执行查询
            - 访问字段时同时兼容大小写：row.FIELD || row.field
            - Table 的 rows 中每行必须有 key 字段
            - 不要添加任何 TypeScript 类型注解
            - 不要使用 import/export 语句
            """;

    @Override
    public Map<String, Object> apply(OverAllState overAllState) {
        Text2SqlState state = Text2SqlState.fromMap(overAllState.data());

        log.info("[RendererNode] 开始生成脚本");
        state.addLog("[RendererNode] 开始生成脚本");

        try {
            String sql = state.getValidatedSql() != null ? state.getValidatedSql() : state.getSql();

            // 调用 LLM 生成脚本
            String llmResponse = chatClient.prompt()
                    .user(userSpec -> userSpec.text(
                            RENDERER_PROMPT
                                    .replace("{sql}", sql)
                                    .replace("{question}", state.getQuestion())
                    ))
                    .call()
                    .content();

            // 提取说明文本
            String explanation = extractExplanation(llmResponse);

            // 提取 JavaScript 代码
            String scriptCode = extractScriptCode(llmResponse);

            state.setScriptCode(scriptCode);
            state.setExplanation(explanation);
            state.addLog("[RendererNode] 脚本生成完成");

            log.info("[RendererNode] 脚本生成完成");

            return state.toMap();

        } catch (Exception e) {
            log.error("[RendererNode] 脚本生成失败", e);
            state.addLog("[RendererNode] 失败: " + e.getMessage());
            state.recordError("RendererNode", classifyError(e), e.getMessage(),
                    "可视化脚本生成失败，请稍后重试。", true);

            state.setScriptCode(generateErrorScript(e.getMessage()));
            state.setExplanation("脚本生成失败：" + e.getMessage());

            return state.toMap();
        }
    }

    private String classifyError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (msg.contains("timeout") || msg.contains("connect")) return "NETWORK_ERROR";
        return "LLM_ERROR";
    }

    /**
     * 从 LLM 响应中提取说明文本（```javascript 之前的部分）
     */
    private String extractExplanation(String llmResponse) {
        if (llmResponse == null || llmResponse.isEmpty()) {
            return "基于 Graph 编排生成的可视化脚本（LIMIT 200）";
        }

        int codeBlockIndex = llmResponse.indexOf("```javascript");
        if (codeBlockIndex == -1) {
            codeBlockIndex = llmResponse.indexOf("```");
        }

        if (codeBlockIndex > 0) {
            String explanation = llmResponse.substring(0, codeBlockIndex).trim();
            if (!explanation.isEmpty()) {
                log.info("[RendererNode] 提取到说明，长度: {}", explanation.length());
                return explanation;
            }
        }

        return "基于 Graph 编排生成的可视化脚本（LIMIT 200）";
    }

    /**
     * 从 LLM 响应中提取 JavaScript 代码
     */
    private String extractScriptCode(String llmResponse) {
        // 尝试提取 ```javascript ... ``` 代码块
        Pattern pattern = Pattern.compile("```javascript\\s*([\\s\\S]*?)```");
        Matcher matcher = pattern.matcher(llmResponse);

        if (matcher.find()) {
            String code = matcher.group(1).trim();
            log.info("[RendererNode] 成功提取脚本代码，长度: {}", code.length());
            return code;
        }

        // 尝试提取 ``` ... ``` 代码块（没有语言标记）
        Pattern genericPattern = Pattern.compile("```\\s*([\\s\\S]*?)```");
        Matcher genericMatcher = genericPattern.matcher(llmResponse);

        if (genericMatcher.find()) {
            String code = genericMatcher.group(1).trim();
            if (code.contains("async function")) {
                log.info("[RendererNode] 从通用代码块中提取脚本，长度: {}", code.length());
                return code;
            }
        }

        // 如果没有代码块但有 async function，返回全部内容
        if (llmResponse.contains("async function")) {
            log.warn("[RendererNode] 未找到代码块标记，但发现 async function，返回全部内容");
            return llmResponse;
        }

        // 兜底：返回整个响应
        log.warn("[RendererNode] 未找到 JavaScript 代码块，返回原始响应");
        return llmResponse;
    }

    /**
     * 生成错误提示脚本
     */
    private String generateErrorScript(String errorMessage) {
        return """
                async function generateData(mcpClient) {
                    return {
                        componentType: 'DataPoint',
                        propertyData: {
                            value: 'ERROR',
                            label: '%s'
                        }
                    };
                }
                """.formatted(errorMessage);
    }
}
