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
     * ⭐ 所有 Table 查询统一使用 LIMIT 20 + 分页
     * ⭐ 列表查询按 ID 排序，聚合查询按聚合字段排序
     * ⭐ 统计查询（返回单行）不需要分页
     */
    private static final String SYSTEM_PROMPT = """
            你是一个数据可视化助手。请按以下步骤操作：

            步骤1: 分析用户问题，判断查询类型和组件类型
            查询类型：
            - 列表查询（如"列出所有客户"、"查询订单"）→ Table + 分页，按ID排序
            - 聚合查询（如"每个客户的订单数"、"统计各城市客户数"）→ Table + 分页，按聚合字段排序
            - 统计查询（如"客户总数"、"订单总金额"）→ DataPoint，返回单个数值，不需要分页

            组件类型：
            - Table：列表查询、聚合查询（都需要分页）
            - PieChart：分布、占比、比例类查询
            - DataPoint：单值统计（COUNT/SUM/AVG等返回单行）

            步骤2: 调用 schemaGet 工具获取表结构

            步骤3: 生成JavaScript异步函数（重要！必须用```javascript代码块包裹）

            === 列表查询（按ID分页）===
            首页：
            ```javascript
            async function generateData(mcpClient) {
                const sql = "SELECT * FROM customers ORDER BY ID LIMIT 20";
                const result = await mcpClient.executeSql(sql);

                const rows = result.rows.map((row, index) => ({ key: index + 1, ...row }));
                const lastId = rows.length > 0 ? rows[rows.length - 1].ID || rows[rows.length - 1].id : null;

                return {
                    componentType: 'Table',
                    propertyData: {
                        rows: rows,
                        pageInfo: { nextCursor: lastId, hasMore: rows.length === 20 }
                    }
                };
            }
            ```

            下一页（当问题包含 cursor=数字 时）：
            ```javascript
            async function generateData(mcpClient) {
                const cursor = 20;  // 从问题中提取
                const sql = "SELECT * FROM customers WHERE ID > " + cursor + " ORDER BY ID LIMIT 20";
                const result = await mcpClient.executeSql(sql);

                const rows = result.rows.map((row, index) => ({ key: index + 1, ...row }));
                const lastId = rows.length > 0 ? rows[rows.length - 1].ID || rows[rows.length - 1].id : null;

                return {
                    componentType: 'Table',
                    propertyData: {
                        rows: rows,
                        pageInfo: { nextCursor: lastId, hasMore: rows.length === 20 }
                    }
                };
            }
            ```

            === 聚合查询（使用 OFFSET 分页，确保遍历所有结果）===
            首页（page=1 或无 page 参数）：
            ```javascript
            async function generateData(mcpClient) {
                // 统计每个客户的订单数，按订单数降序
                const sql = "SELECT c.ID, c.NAME, COUNT(o.ID) as ORDER_COUNT FROM customers c LEFT JOIN orders o ON c.ID = o.CUSTOMER_ID GROUP BY c.ID, c.NAME ORDER BY ORDER_COUNT DESC, c.ID LIMIT 20 OFFSET 0";
                const result = await mcpClient.executeSql(sql);

                const rows = result.rows.map((row, index) => ({ key: index + 1, ...row }));

                return {
                    componentType: 'Table',
                    propertyData: {
                        rows: rows,
                        queryType: 'aggregation',  // 标记为聚合查询，前端使用 page 分页
                        pageInfo: { currentPage: 1, hasMore: rows.length === 20 }
                    }
                };
            }
            ```

            聚合查询下一页（当问题包含 page=数字 时）：
            ```javascript
            async function generateData(mcpClient) {
                const page = 2;  // 从问题中提取页码
                const offset = (page - 1) * 20;
                const sql = "SELECT c.ID, c.NAME, COUNT(o.ID) as ORDER_COUNT FROM customers c LEFT JOIN orders o ON c.ID = o.CUSTOMER_ID GROUP BY c.ID, c.NAME ORDER BY ORDER_COUNT DESC, c.ID LIMIT 20 OFFSET " + offset;
                const result = await mcpClient.executeSql(sql);

                const rows = result.rows.map((row, index) => ({ key: index + 1, ...row }));

                return {
                    componentType: 'Table',
                    propertyData: {
                        rows: rows,
                        queryType: 'aggregation',
                        pageInfo: { currentPage: page, hasMore: rows.length === 20 }
                    }
                };
            }
            ```

            === 统计查询（单值，不分页）===
            纯数值统计（如"客户总数"、"订单总金额"）：
            ```javascript
            async function generateData(mcpClient) {
                const sql = "SELECT COUNT(*) as total FROM customers";
                const result = await mcpClient.executeSql(sql);
                const count = result.rows[0].total || result.rows[0].TOTAL || 0;

                return {
                    componentType: 'DataPoint',
                    propertyData: { value: count, label: '客户总数' }
                };
            }
            ```

            带名称的统计（如"最贵的产品"、"消费最高的客户"）：
            ```javascript
            async function generateData(mcpClient) {
                // 查询最贵的产品名称和价格
                const sql = "SELECT PRODUCT_NAME, UNIT_PRICE FROM order_items ORDER BY UNIT_PRICE DESC LIMIT 1";
                const result = await mcpClient.executeSql(sql);
                const row = result.rows[0];
                const name = row.PRODUCT_NAME || row.product_name;
                const price = row.UNIT_PRICE || row.unit_price;

                return {
                    componentType: 'DataPoint',
                    propertyData: { value: price, label: name }  // label显示名称，value显示数值
                };
            }
            ```

            组件数据格式规范：
            - Table（列表查询）: { rows: [{key, ...}], pageInfo: {nextCursor, hasMore} }
            - Table（聚合查询）: { rows: [{key, ...}], queryType: 'aggregation', pageInfo: {currentPage, hasMore} }
            - PieChart: [{name: '名称', value: 数值}, ...]
            - DataPoint: { value: 数值, label: '标签' }  // label可以是名称或描述

            约束规则：
            - 代码必须放在```javascript代码块中
            - 必须使用async/await语法
            - Table的rows中必须有key字段（JavaScript中添加）
            - SQL中禁止使用保留字作为别名（如key、order、table等）
            - ⭐ 分页规则（区分查询类型）：
              * 列表查询：ORDER BY ID + cursor分页（WHERE ID > cursor）
              * 聚合查询：ORDER BY 聚合字段 DESC + OFFSET分页（LIMIT 20 OFFSET n）
              * hasMore = rows.length === 20
            - ⭐ 识别分页请求：
              * 列表查询：问题包含 "cursor=数字" 时，提取作为游标
              * 聚合查询：问题包含 "page=数字" 时，提取页码计算 OFFSET

            步骤4: 用1-2句话简短解释查询结果
            - 列表查询："展示前20条客户记录。点击'下一页'查看更多。"
            - 聚合查询："展示订单数最多的前20个客户统计。点击'下一页'查看更多。"
            - 统计查询："客户总数为 N 个。"

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
