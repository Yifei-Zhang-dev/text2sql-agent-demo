package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * MCP 工具服务 - 封装对 MCP Server 的 HTTP 调用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolService {

    private final WebClient mcpWebClient;

    /**
     * 调用 schema.get 工具
     */
    public String getSchema(String tableName) {
        log.info("调用 MCP Tool: schema.get, table={}", tableName);

        try {
            Map<String, Object> response = mcpWebClient.post()
                    .uri("/mcp/tools/schema.get")
                    .bodyValue(Map.of("table", tableName))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("schema.get 响应: {}", response);
            return formatSchemaResponse(response);
        } catch (Exception e) {
            log.error("schema.get 调用失败", e);
            return "获取表结构失败: " + e.getMessage();
        }
    }

    /**
     * 调用 sql.run 工具
     */
    public String runSql(String sql) {
        log.info("调用 MCP Tool: sql.run, sql={}", sql);

        try {
            Map<String, Object> response = mcpWebClient.post()
                    .uri("/mcp/tools/sql.run")
                    .bodyValue(Map.of("sql", sql))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("sql.run 响应: {}", response);
            return formatSqlResponse(response);
        } catch (Exception e) {
            log.error("sql.run 调用失败", e);
            return "SQL 执行失败: " + e.getMessage();
        }
    }

    /**
     * 格式化 schema 响应为可读文本
     */
    private String formatSchemaResponse(Map<String, Object> response) {
        if (response == null) {
            return "无响应";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("表名: ").append(response.get("tableName")).append("\n");
        sb.append("列信息:\n");

        Object columns = response.get("columns");
        if (columns instanceof java.util.List) {
            for (Object col : (java.util.List<?>) columns) {
                if (col instanceof Map) {
                    Map<String, Object> column = (Map<String, Object>) col;
                    sb.append("  - ").append(column.get("name"))
                            .append(" (").append(column.get("type")).append(")")
                            .append(": ").append(column.get("comment"))
                            .append("\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * 格式化 SQL 响应为可读文本
     */
    private String formatSqlResponse(Map<String, Object> response) {
        if (response == null) {
            return "无响应";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("查询结果 (共 ").append(response.get("rowCount")).append(" 行):\n");

        Object columns = response.get("columns");
        Object rows = response.get("rows");

        if (columns instanceof java.util.List && rows instanceof java.util.List) {
            java.util.List<?> colList = (java.util.List<?>) columns;
            java.util.List<?> rowList = (java.util.List<?>) rows;

            // 打印列名
            sb.append("列: ").append(String.join(", ", colList.stream()
                    .map(Object::toString).toArray(String[]::new))).append("\n");

            // 打印行数据
            for (Object row : rowList) {
                if (row instanceof java.util.List) {
                    sb.append("  ").append(row).append("\n");
                }
            }
        }

        return sb.toString();
    }
}
