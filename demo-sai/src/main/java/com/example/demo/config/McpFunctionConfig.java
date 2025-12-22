package com.example.demo.config;

import com.example.demo.service.McpToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

/**
 * MCP 工具函数配置 - 将 MCP Server 工具注册为 Spring AI Function
 */
@Configuration
@RequiredArgsConstructor
public class McpFunctionConfig {

    private final McpToolService mcpToolService;

    /**
     * schema.get 工具函数
     */
    @Bean
    @Description("获取数据库表的结构信息（字段名、类型、注释）。可用表：customers, orders, order_items")
    public Function<SchemaGetRequest, String> schemaGet() {
        return request -> mcpToolService.getSchema(request.table());
    }

    /**
     * sql.run 工具函数
     */
    @Bean
    @Description("执行只读 SQL 查询（仅支持 SELECT 语句），返回查询结果")
    public Function<SqlRunRequest, String> sqlRun() {
        return request -> mcpToolService.runSql(request.sql());
    }

    /**
     * Schema Get 请求参数
     */
    public record SchemaGetRequest(String table) {}

    /**
     * SQL Run 请求参数
     */
    public record SqlRunRequest(String sql) {}
}
