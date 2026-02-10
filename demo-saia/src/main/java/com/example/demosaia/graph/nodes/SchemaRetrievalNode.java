package com.example.demosaia.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.example.demosaia.graph.state.Text2SqlState;
import com.example.demosaia.service.McpToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

/**
 * Schema Retrieval Node - 表结构获取节点
 * 职责：根据问题推断需要的表，调用 MCP Server 获取表结构
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaRetrievalNode implements Function<OverAllState, Map<String, Object>> {

    private final McpToolService mcpToolService;

    @Override
    public Map<String, Object> apply(OverAllState overAllState) {
        Text2SqlState state = Text2SqlState.fromMap(overAllState.data());

        log.info("[SchemaRetrievalNode] 开始获取表结构");
        state.addLog("[SchemaRetrievalNode] 开始获取表结构");

        try {
            // 根据问题推断需要的表（可能多张）
            List<String> tableNames = inferTableNames(state.getQuestion());

            log.info("[SchemaRetrievalNode] 推断需要的表: {}", tableNames);
            state.addLog("[SchemaRetrievalNode] 推断表名: " + tableNames);

            // 获取所有需要的表结构
            StringBuilder allSchemaText = new StringBuilder();
            for (String tableName : tableNames) {
                String schemaText = mcpToolService.getSchema(tableName);
                allSchemaText.append(schemaText).append("\n");
            }

            // 存入 schema Map，供 ComplexSqlGeneratorNode 使用
            state.setSchema(Map.of(
                    "tableName", String.join(", ", tableNames),
                    "schemaText", allSchemaText.toString()
            ));
            state.addLog("[SchemaRetrievalNode] 成功获取表结构");

            log.info("[SchemaRetrievalNode] 表结构获取完成");

            return state.toMap();

        } catch (Exception e) {
            log.error("[SchemaRetrievalNode] 获取表结构失败", e);
            state.addLog("[SchemaRetrievalNode] 失败: " + e.getMessage());
            state.recordError("SchemaRetrievalNode", "NETWORK_ERROR", e.getMessage(),
                    "获取表结构失败，MCP Server 可能不可用，请稍后重试。", true);

            state.setSchema(Map.of());
            return state.toMap();
        }
    }

    /**
     * 根据问题推断需要的表名（支持多表）
     * 复杂查询通常涉及多张表的 JOIN
     */
    private List<String> inferTableNames(String question) {
        String q = question.toLowerCase();
        Set<String> tables = new LinkedHashSet<>();

        // 产品/销售/商品相关 → order_items（注意：没有独立的 products 表）
        if (q.contains("产品") || q.contains("商品") || q.contains("销售") || q.contains("销量")
                || q.contains("product") || q.contains("sale")) {
            tables.add("order_items");
            tables.add("orders");  // 销售分析通常需要订单信息
        }

        // 客户相关
        if (q.contains("客户") || q.contains("customer") || q.contains("用户")) {
            tables.add("customers");
        }

        // 订单相关
        if (q.contains("订单") || q.contains("order") || q.contains("月") || q.contains("趋势")) {
            tables.add("orders");
        }

        // 订单明细相关
        if (q.contains("订单项") || q.contains("订单明细") || q.contains("item")) {
            tables.add("order_items");
        }

        // 如果涉及多个维度（如客户+订单），确保关联表都在
        if (tables.contains("customers") && (q.contains("订单") || q.contains("消费") || q.contains("购买"))) {
            tables.add("orders");
        }

        // 默认：如果没匹配到任何关键词，返回所有表
        if (tables.isEmpty()) {
            return List.of("customers", "orders", "order_items");
        }

        return new ArrayList<>(tables);
    }
}
