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
 * Simple SQL Generator Node - 简单 SQL 生成节点
 * 职责：为简单查询生成 SQL（列表查询、单表统计）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleSqlGeneratorNode implements Function<OverAllState, Map<String, Object>> {

    private final ChatClient chatClient;

    private static final String SIMPLE_SQL_PROMPT = """
            你是 SQL 专家。根据用户问题生成 SQL 语句。

            【数据库信息】
            - 数据库类型：H2
            - 数据库中只有 3 张表：customers、orders、order_items
            - ⚠️ 没有 products 表！产品信息在 order_items 表的 PRODUCT_NAME 字段中
            - H2 日期函数：YEAR(date)、MONTH(date)、FORMATDATETIME(date, 'yyyy-MM')

            【可用表及字段】
            - customers (ID, NAME, EMAIL, PHONE, CITY, CREATED_AT) - 客户信息
            - orders (ID, CUSTOMER_ID, ORDER_DATE, TOTAL_AMOUNT, STATUS) - 订单记录
            - order_items (ID, ORDER_ID, PRODUCT_NAME, QUANTITY, UNIT_PRICE, SUBTOTAL) - 订单明细

            【生成规则】
            1. 列表查询：使用 ORDER BY ID LIMIT 200
            2. 统计查询：使用 COUNT(*)/SUM()/AVG() 等聚合函数
            3. 只生成 SQL，不要解释
            4. 不要使用 ``` 代码块包裹
            5. 城市名筛选必须用 LIKE 模糊匹配（数据库中城市带"市"后缀，如"北京市"、"上海市"），例如：WHERE CITY LIKE '%北京%'

            【示例】
            问题：列出所有客户
            SQL：SELECT * FROM customers ORDER BY ID LIMIT 200

            问题：订单总金额是多少
            SQL：SELECT SUM(TOTAL_AMOUNT) as TOTAL FROM orders

            问题：查询北京的客户
            SQL：SELECT * FROM customers WHERE CITY LIKE '%北京%' ORDER BY ID LIMIT 200

            【用户问题】
            {question}
            """;

    @Override
    public Map<String, Object> apply(OverAllState overAllState) {
        Text2SqlState state = Text2SqlState.fromMap(overAllState.data());

        log.info("[SimpleSqlGeneratorNode] 开始生成简单 SQL");
        state.addLog("[SimpleSqlGeneratorNode] 开始生成 SQL");

        try {
            // 生成 SQL
            String sql = chatClient.prompt()
                    .user(userSpec -> userSpec.text(
                            SIMPLE_SQL_PROMPT
                                    .replace("{question}", state.getQuestion())
                    ))
                    .call()
                    .content()
                    .trim();

            // 清理可能的代码块标记
            sql = cleanSqlOutput(sql);

            state.setSql(sql);
            state.addLog("[SimpleSqlGeneratorNode] 生成的 SQL: " + sql);

            log.info("[SimpleSqlGeneratorNode] SQL 生成完成: {}", sql);

            return state.toMap();

        } catch (Exception e) {
            log.error("[SimpleSqlGeneratorNode] SQL 生成失败", e);
            state.addLog("[SimpleSqlGeneratorNode] 失败: " + e.getMessage());
            state.recordError("SimpleSqlGeneratorNode", classifyError(e), e.getMessage(),
                    "SQL 生成失败，请尝试重新表述问题或稍后重试。", true);

            state.setSql("SELECT 'Error' as MESSAGE");
            return state.toMap();
        }
    }

    private String classifyError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (msg.contains("timeout") || msg.contains("connect")) return "NETWORK_ERROR";
        return "LLM_ERROR";
    }

    private String cleanSqlOutput(String sql) {
        return sql.replace("```sql", "")
                .replace("```", "")
                .replace("SQL:", "")
                .trim();
    }
}
