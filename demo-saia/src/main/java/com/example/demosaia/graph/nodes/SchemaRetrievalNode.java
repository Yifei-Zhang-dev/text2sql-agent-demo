package com.example.demosaia.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.example.demosaia.graph.state.Text2SqlState;
import com.example.demosaia.service.McpToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
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
            // 根据问题推断需要的表
            String tableName = inferTableName(state.getQuestion());

            log.info("[SchemaRetrievalNode] 推断需要的表: {}", tableName);
            state.addLog("[SchemaRetrievalNode] 推断表名: " + tableName);

            // 调用 MCP Server 获取表结构（返回格式化的 String）
            String schemaText = mcpToolService.getSchema(tableName);

            // 存入 schema Map，供 ComplexSqlGeneratorNode 使用
            state.setSchema(Map.of("tableName", tableName, "schemaText", schemaText));
            state.addLog("[SchemaRetrievalNode] 成功获取表结构");

            log.info("[SchemaRetrievalNode] 表结构获取完成");

            return state.toMap();

        } catch (Exception e) {
            log.error("[SchemaRetrievalNode] 获取表结构失败", e);
            state.addLog("[SchemaRetrievalNode] 失败: " + e.getMessage());

            state.setSchema(Map.of());
            return state.toMap();
        }
    }

    /**
     * 根据问题推断需要的表名
     * 简化版本：基于关键词匹配
     */
    private String inferTableName(String question) {
        String q = question.toLowerCase();

        if (q.contains("客户") || q.contains("customer")) {
            return "customers";
        } else if (q.contains("订单") && (q.contains("项") || q.contains("商品") || q.contains("product"))) {
            return "order_items";
        } else if (q.contains("订单") || q.contains("order")) {
            return "orders";
        }

        // 默认返回 customers（最常用）
        return "customers";
    }
}
