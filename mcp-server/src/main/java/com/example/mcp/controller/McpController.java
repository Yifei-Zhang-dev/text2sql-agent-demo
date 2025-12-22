package com.example.mcp.controller;

import com.example.mcp.dto.*;
import com.example.mcp.service.DatabaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpController {

    private final DatabaseService databaseService;

    /**
     * GET /mcp/tools - 列出所有可用工具
     */
    @GetMapping("/tools")
    public ToolListResponse listTools() {
        ToolListResponse response = new ToolListResponse();
        response.setVersion("0.1");

        List<ToolListResponse.Tool> tools = new ArrayList<>();

        // Tool 1: schema.get
        ToolListResponse.Tool schemaTool = new ToolListResponse.Tool();
        schemaTool.setName("schema.get");
        schemaTool.setDescription("获取数据库表的结构信息（字段名、类型、注释）");
        ToolListResponse.InputSchema schemaInputSchema = new ToolListResponse.InputSchema();
        schemaInputSchema.setType("object");
        schemaInputSchema.setRequired(List.of("table"));
        Map<String, Object> schemaProps = new HashMap<>();
        schemaProps.put("table", Map.of("type", "string", "description", "表名"));
        schemaInputSchema.setProperties(schemaProps);
        schemaTool.setInputSchema(schemaInputSchema);
        tools.add(schemaTool);

        // Tool 2: sql.run
        ToolListResponse.Tool sqlRunTool = new ToolListResponse.Tool();
        sqlRunTool.setName("sql.run");
        sqlRunTool.setDescription("执行只读 SQL 查询（仅支持 SELECT 语句）");
        ToolListResponse.InputSchema sqlInputSchema = new ToolListResponse.InputSchema();
        sqlInputSchema.setType("object");
        sqlInputSchema.setRequired(List.of("sql"));
        Map<String, Object> sqlProps = new HashMap<>();
        sqlProps.put("sql", Map.of("type", "string", "description", "只读 SQL 查询语句"));
        sqlInputSchema.setProperties(sqlProps);
        sqlRunTool.setInputSchema(sqlInputSchema);
        tools.add(sqlRunTool);

        // Tool 3: sql.validate (可选)
        ToolListResponse.Tool sqlValidateTool = new ToolListResponse.Tool();
        sqlValidateTool.setName("sql.validate");
        sqlValidateTool.setDescription("验证 SQL 语句的语法正确性和安全性");
        ToolListResponse.InputSchema validateInputSchema = new ToolListResponse.InputSchema();
        validateInputSchema.setType("object");
        validateInputSchema.setRequired(List.of("sql"));
        Map<String, Object> validateProps = new HashMap<>();
        validateProps.put("sql", Map.of("type", "string", "description", "待验证的 SQL 语句"));
        validateInputSchema.setProperties(validateProps);
        sqlValidateTool.setInputSchema(validateInputSchema);
        tools.add(sqlValidateTool);

        response.setTools(tools);
        return response;
    }

    /**
     * POST /mcp/tools/schema.get - 获取表结构（真实数据库）
     */
    @PostMapping("/tools/schema.get")
    public SchemaResponse getSchema(@RequestBody SchemaRequest request) {
        log.info("=== 收到 schema.get 请求 ===");
        log.info("请求体: {}", request);
        log.info("表名: {}", request.getTable());
        
        try {
            SchemaResponse response = databaseService.getTableSchema(request.getTable());
            log.info("schema.get 成功响应: {}", response);
            return response;
        } catch (Exception e) {
            log.error("=== schema.get 处理失败 ===", e);
            throw e;
        }
    }

    /**
     * POST /mcp/tools/sql.run - 执行 SQL（真实数据库）
     */
    @PostMapping("/tools/sql.run")
    public SqlResponse runSql(@RequestBody SqlRequest request) {
        log.info("=== 收到 sql.run 请求 ===");
        log.info("SQL: {}", request.getSql());
        
        try {
            SqlResponse response = databaseService.executeSql(request.getSql());
            log.info("sql.run 成功响应: {}", response);
            return response;
        } catch (Exception e) {
            log.error("=== sql.run 处理失败 ===", e);
            throw e;
        }
    }

    /**
     * POST /mcp/tools/sql.validate - 验证 SQL
     */
    @PostMapping("/tools/sql.validate")
    public Map<String, Object> validateSql(@RequestBody SqlRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("sql", request.getSql());

        boolean isValid = databaseService.validateSql(request.getSql());

        if (isValid) {
            result.put("valid", true);
            result.put("message", "SQL 语法验证通过");
        } else {
            result.put("valid", false);
            result.put("error", "SQL 包含非法操作或危险关键字");
        }

        return result;
    }
}
