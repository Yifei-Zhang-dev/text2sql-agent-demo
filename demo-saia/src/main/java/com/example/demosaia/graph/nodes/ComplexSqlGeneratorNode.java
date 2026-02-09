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

            【表结构】
            {schema}

            【生成规则】
            1. 支持 JOIN、GROUP BY、HAVING
            2. 聚合查询使用 LIMIT 20
            3. 按聚合字段排序（如 ORDER BY COUNT DESC）
            4. 只生成 SQL，不要解释
            5. 不要使用 ``` 代码块包裹

            【示例】
            问题：每个客户的订单数
            SQL：SELECT c.ID, c.NAME, COUNT(o.ID) as ORDER_COUNT FROM customers c LEFT JOIN orders o ON c.ID = o.CUSTOMER_ID GROUP BY c.ID, c.NAME ORDER BY ORDER_COUNT DESC LIMIT 20

            【用户问题】
            {question}

            {paginationHint}
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

            // 处理分页参数
            String paginationHint = "";
            if (state.getPaginationParam() != null && state.getPaginationParam().startsWith("page=")) {
                String page = state.getPaginationParam().substring(5);
                int pageNum = Integer.parseInt(page);
                int offset = (pageNum - 1) * 20;
                paginationHint = "注意：这是第 " + page + " 页，使用 LIMIT 20 OFFSET " + offset;
            }

            // 构建最终 prompt
            String finalSchemaStr = schemaStr;
            String finalPaginationHint = paginationHint;

            // 生成 SQL
            String sql = chatClient.prompt()
                    .user(userSpec -> userSpec.text(
                            COMPLEX_SQL_PROMPT
                                    .replace("{schema}", finalSchemaStr)
                                    .replace("{question}", state.getQuestion())
                                    .replace("{paginationHint}", finalPaginationHint)
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
