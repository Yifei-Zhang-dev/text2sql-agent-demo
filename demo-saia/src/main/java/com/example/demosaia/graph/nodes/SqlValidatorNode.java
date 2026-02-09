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
 * SQL Validator Node - SQL 验证节点
 * 职责：验证 SQL 语法正确性
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqlValidatorNode implements Function<OverAllState, Map<String, Object>> {

    private final McpToolService mcpToolService;

    @Override
    public Map<String, Object> apply(OverAllState overAllState) {
        Text2SqlState state = Text2SqlState.fromMap(overAllState.data());

        log.info("[SqlValidatorNode] 开始验证 SQL");
        state.addLog("[SqlValidatorNode] 开始验证 SQL");

        String sql = state.getSql();

        if (sql == null || sql.isEmpty()) {
            log.warn("[SqlValidatorNode] SQL 为空，跳过验证");
            state.setValidatedSql(sql);
            state.setIsValid(false);
            state.setValidationError("SQL 为空");
            return state.toMap();
        }

        try {
            // 简单验证：尝试执行 LIMIT 1 测试
            String testSql = addLimitIfNeeded(sql);
            mcpToolService.runSql(testSql);

            state.setValidatedSql(sql);
            state.setIsValid(true);
            state.addLog("[SqlValidatorNode] SQL 验证通过");

            log.info("[SqlValidatorNode] SQL 验证通过");

            return state.toMap();

        } catch (Exception e) {
            log.warn("[SqlValidatorNode] SQL 验证失败: {}", e.getMessage());
            state.addLog("[SqlValidatorNode] 验证失败: " + e.getMessage());

            // 验证失败，但仍然继续（由 Renderer 处理错误）
            state.setValidatedSql(sql);
            state.setIsValid(false);
            state.setValidationError(e.getMessage());

            return state.toMap();
        }
    }

    /**
     * 为测试 SQL 添加 LIMIT 1（如果还没有 LIMIT）
     */
    private String addLimitIfNeeded(String sql) {
        String sqlUpper = sql.toUpperCase();
        if (sqlUpper.contains("LIMIT")) {
            return sql.replaceAll("(?i)LIMIT\\s+\\d+", "LIMIT 1");
        } else {
            return sql + " LIMIT 1";
        }
    }
}
