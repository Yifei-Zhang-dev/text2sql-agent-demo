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
            - 表：customers（客户表）、orders（订单表）、order_items（订单项表）
            - H2 日期函数：YEAR(date)、MONTH(date)、FORMATDATETIME(date, 'yyyy-MM')

            【生成规则】
            1. 列表查询：使用 ORDER BY ID LIMIT 20
            2. 统计查询：使用 COUNT(*)/SUM()/AVG() 等聚合函数
            3. 只生成 SQL，不要解释
            4. 不要使用 ``` 代码块包裹

            【示例】
            问题：列出所有客户
            SQL：SELECT * FROM customers ORDER BY ID LIMIT 20

            问题：订单总金额是多少
            SQL：SELECT SUM(TOTAL_AMOUNT) as TOTAL FROM orders

            【用户问题】
            {question}

            {paginationHint}
            """;

    @Override
    public Map<String, Object> apply(OverAllState overAllState) {
        Text2SqlState state = Text2SqlState.fromMap(overAllState.data());

        log.info("[SimpleSqlGeneratorNode] 开始生成简单 SQL");
        state.addLog("[SimpleSqlGeneratorNode] 开始生成 SQL");

        try {
            // 处理分页参数
            String paginationHint = "";
            if (state.getPaginationParam() != null && !state.getPaginationParam().isEmpty()) {
                if (state.getPaginationParam().startsWith("cursor=")) {
                    String cursor = state.getPaginationParam().substring(7);
                    paginationHint = "注意：这是翻页请求，使用 WHERE ID > " + cursor + " 进行分页";
                }
            }

            // 构建最终 prompt
            String finalPaginationHint = paginationHint;

            // 生成 SQL
            String sql = chatClient.prompt()
                    .user(userSpec -> userSpec.text(
                            SIMPLE_SQL_PROMPT
                                    .replace("{question}", state.getQuestion())
                                    .replace("{paginationHint}", finalPaginationHint)
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
