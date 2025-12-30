package com.example.demosaia.service;

import com.example.demosaia.dto.QueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * MCP 工具服务 - 封装对 MCP Server 的 HTTP 调用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolService {

    private final WebClient mcpWebClient;

    // 用于跟踪最后执行的 SQL
    private static final ThreadLocal<String> lastExecutedSql = new ThreadLocal<>();

    // 用于跟踪最后的查询结果
    private static final ThreadLocal<QueryResult> lastQueryResult = new ThreadLocal<>();

    /**
     * 调用 schema.get 工具
     */
    public String getSchema(String tableName) {
        log.info("=== 调用 MCP Tool: schema.get ===");
        log.info("请求参数: table={}", tableName);

        try {
            Map<String, Object> requestBody = Map.of("table", tableName);
            log.info("请求体: {}", requestBody);

            Map<String, Object> response = mcpWebClient.post()
                    .uri("/mcp/tools/schema.get")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            clientResponse -> {
                                log.error("HTTP 错误状态: {}", clientResponse.statusCode());
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(body -> {
                                            log.error("错误响应体: {}", body);
                                            return Mono.error(new RuntimeException(
                                                    "HTTP " + clientResponse.statusCode() + ": " + body));
                                        });
                            })
                    .bodyToMono(Map.class)
                    .doOnError(error -> log.error("请求失败: {}", error.getMessage()))
                    .block();

            log.info("schema.get 成功响应: {}", response);
            return formatSchemaResponse(response);
        } catch (WebClientResponseException e) {
            log.error("=== schema.get HTTP 错误 ===");
            log.error("状态码: {}", e.getStatusCode());
            log.error("响应头: {}", e.getHeaders());
            log.error("响应体: {}", e.getResponseBodyAsString());
            return "获取表结构失败 [HTTP " + e.getStatusCode() + "]: " + e.getResponseBodyAsString();
        } catch (Exception e) {
            log.error("=== schema.get 调用异常 ===", e);
            return "获取表结构失败: " + e.getMessage();
        }
    }

    /**
     * 调用 sql.run 工具
     */
    public String runSql(String sql) {
        log.info("=== 调用 MCP Tool: sql.run ===");
        log.info("SQL: {}", sql);

        // 保存 SQL 到 ThreadLocal
        lastExecutedSql.set(sql);

        try {
            Map<String, Object> requestBody = Map.of("sql", sql);
            log.info("请求体: {}", requestBody);

            Map<String, Object> response = mcpWebClient.post()
                    .uri("/mcp/tools/sql.run")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            clientResponse -> {
                                log.error("HTTP 错误状态: {}", clientResponse.statusCode());
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(body -> {
                                            log.error("错误响应体: {}", body);
                                            return Mono.error(new RuntimeException(
                                                    "HTTP " + clientResponse.statusCode() + ": " + body));
                                        });
                            })
                    .bodyToMono(Map.class)
                    .doOnError(error -> log.error("请求失败: {}", error.getMessage()))
                    .block();

            log.info("sql.run 成功响应: {}", response);

            // 保存结构化结果到 ThreadLocal
            saveQueryResult(response);

            return formatSqlResponse(response);
        } catch (WebClientResponseException e) {
            log.error("=== sql.run HTTP 错误 ===");
            log.error("状态码: {}", e.getStatusCode());
            log.error("响应头: {}", e.getHeaders());
            log.error("响应体: {}", e.getResponseBodyAsString());
            return "SQL 执行失败 [HTTP " + e.getStatusCode() + "]: " + e.getResponseBodyAsString();
        } catch (Exception e) {
            log.error("=== sql.run 调用异常 ===", e);
            return "SQL 执行失败: " + e.getMessage();
        }
    }

    /**
     * 保存查询结果到 ThreadLocal
     */
    @SuppressWarnings("unchecked")
    private void saveQueryResult(Map<String, Object> response) {
        if (response == null) {
            return;
        }

        try {
            List<String> columns = (List<String>) response.get("columns");
            List<List<Object>> rows = (List<List<Object>>) response.get("rows");
            Integer rowCount = (Integer) response.get("rowCount");

            if (columns != null && rows != null && rowCount != null) {
                QueryResult queryResult = new QueryResult(columns, rows, rowCount);
                lastQueryResult.set(queryResult);
                log.info("已保存查询结果: {} 行, {} 列", rowCount, columns.size());
            }
        } catch (Exception e) {
            log.warn("保存查询结果失败: {}", e.getMessage());
        }
    }

    /**
     * 获取最后执行的 SQL
     */
    public static String getLastExecutedSql() {
        return lastExecutedSql.get();
    }

    /**
     * 获取最后的查询结果
     */
    public static QueryResult getLastQueryResult() {
        return lastQueryResult.get();
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
