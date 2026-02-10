package com.example.demosaia.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.example.demosaia.graph.state.Text2SqlState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;

/**
 * Complex SQL Generator Node - 复杂 SQL 生成节点
 * 职责：为复杂查询生成 SQL（多表关联、聚合统计）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComplexSqlGeneratorNode implements Function<OverAllState, Map<String, Object>> {

    private final ChatClient chatClient;

    private static final String COMPLEX_SQL_PROMPT = """
            你是 SQL 专家。根据用户问题和表结构生成复杂 SQL。

            【数据库信息】
            - 数据库类型：H2
            - H2 日期函数：YEAR(date)、MONTH(date)、FORMATDATETIME(date, 'yyyy-MM')
            - 数据库中只有 3 张表：customers、orders、order_items
            - ⚠️ 没有 products 表！产品信息在 order_items 表的 PRODUCT_NAME 字段中

            【可用表及关系】
            - customers (ID, NAME, EMAIL, PHONE, CITY, CREATED_AT) - 客户信息
            - orders (ID, CUSTOMER_ID, ORDER_DATE, TOTAL_AMOUNT, STATUS) - 订单记录，CUSTOMER_ID 关联 customers.ID
            - order_items (ID, ORDER_ID, PRODUCT_NAME, QUANTITY, UNIT_PRICE, SUBTOTAL) - 订单明细，ORDER_ID 关联 orders.ID

            【查询到的表结构详情】
            {schema}

            【生成规则】
            1. 支持 JOIN、GROUP BY、HAVING
            2. 聚合查询使用 LIMIT 200
            3. 按聚合字段排序（如 ORDER BY COUNT DESC）
            4. 只生成 SQL，不要解释
            5. 不要使用 ``` 代码块包裹
            6. 产品相关查询必须使用 order_items 表（没有 products 表）

            【示例】
            问题：每个客户的订单数
            SQL：SELECT c.ID, c.NAME, COUNT(o.ID) as ORDER_COUNT FROM customers c LEFT JOIN orders o ON c.ID = o.CUSTOMER_ID GROUP BY c.ID, c.NAME ORDER BY ORDER_COUNT DESC LIMIT 200

            问题：各产品销售量排行
            SQL：SELECT PRODUCT_NAME, SUM(QUANTITY) as TOTAL_QTY FROM order_items GROUP BY PRODUCT_NAME ORDER BY TOTAL_QTY DESC LIMIT 200

            【用户问题】
            {question}
            """;

    @Override
    public Map<String, Object> apply(OverAllState overAllState) {
        Text2SqlState state = Text2SqlState.fromMap(overAllState.data());

        log.info("[ComplexSqlGeneratorNode] 开始生成复杂 SQL");
        state.addLog("[ComplexSqlGeneratorNode] 开始生成 SQL");

        try {
            // 获取 schema
            Map<String, Object> schema = state.getSchema();
            String schemaStr;
            if (schema != null && schema.containsKey("schemaText")) {
                schemaStr = (String) schema.get("schemaText");
            } else {
                schemaStr = schema != null ? schema.toString() : "无表结构信息";
            }

            // 构建最终 prompt
            String finalSchemaStr = schemaStr;

            // 生成 SQL
            String sql = chatClient.prompt()
                    .user(userSpec -> userSpec.text(
                            COMPLEX_SQL_PROMPT
                                    .replace("{schema}", finalSchemaStr)
                                    .replace("{question}", state.getQuestion())
                    ))
                    .call()
                    .content()
                    .trim();

            // 清理输出
            sql = cleanSqlOutput(sql);

            state.setSql(sql);
            state.addLog("[ComplexSqlGeneratorNode] 生成的 SQL: " + sql);

            log.info("[ComplexSqlGeneratorNode] SQL 生成完成");

            return state.toMap();

        } catch (Exception e) {
            log.error("[ComplexSqlGeneratorNode] SQL 生成失败", e);
            state.addLog("[ComplexSqlGeneratorNode] 失败: " + e.getMessage());

            state.setSql("SELECT 'Error' as MESSAGE");
            return state.toMap();
        }
    }

    private String cleanSqlOutput(String sql) {
        return sql.replace("```sql", "")
                .replace("```", "")
                .replace("SQL:", "")
                .trim();
    }
}
