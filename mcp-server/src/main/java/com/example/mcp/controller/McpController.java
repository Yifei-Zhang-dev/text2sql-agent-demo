package com.example.mcp.controller;

import com.example.mcp.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/mcp")
public class McpController {

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
     * POST /mcp/tools/schema.get - 获取表结构（Mock 数据）
     */
    @PostMapping("/tools/schema.get")
    public SchemaResponse getSchema(@RequestBody SchemaRequest request) {
        String tableName = request.getTable();

        // Mock 3 张表的 schema
        if ("customers".equalsIgnoreCase(tableName)) {
            return new SchemaResponse("customers", List.of(
                    new SchemaResponse.Column("id", "BIGINT", "客户 ID（主键）"),
                    new SchemaResponse.Column("name", "VARCHAR(100)", "客户姓名"),
                    new SchemaResponse.Column("email", "VARCHAR(255)", "电子邮箱"),
                    new SchemaResponse.Column("phone", "VARCHAR(20)", "联系电话"),
                    new SchemaResponse.Column("created_at", "TIMESTAMP", "创建时间")
            ));
        } else if ("orders".equalsIgnoreCase(tableName)) {
            return new SchemaResponse("orders", List.of(
                    new SchemaResponse.Column("id", "BIGINT", "订单 ID（主键）"),
                    new SchemaResponse.Column("customer_id", "BIGINT", "客户 ID（外键）"),
                    new SchemaResponse.Column("order_date", "DATE", "订单日期"),
                    new SchemaResponse.Column("total_amount", "DECIMAL(10,2)", "订单总金额"),
                    new SchemaResponse.Column("status", "VARCHAR(20)", "订单状态（pending/paid/shipped/completed）")
            ));
        } else if ("order_items".equalsIgnoreCase(tableName)) {
            return new SchemaResponse("order_items", List.of(
                    new SchemaResponse.Column("id", "BIGINT", "订单项 ID（主键）"),
                    new SchemaResponse.Column("order_id", "BIGINT", "订单 ID（外键）"),
                    new SchemaResponse.Column("product_name", "VARCHAR(200)", "商品名称"),
                    new SchemaResponse.Column("quantity", "INT", "购买数量"),
                    new SchemaResponse.Column("unit_price", "DECIMAL(10,2)", "单价"),
                    new SchemaResponse.Column("subtotal", "DECIMAL(10,2)", "小计金额")
            ));
        } else {
            throw new IllegalArgumentException("表 '" + tableName + "' 不存在，可用表：customers, orders, order_items");
        }
    }

    /**
     * POST /mcp/tools/sql.run - 执行 SQL（Mock 数据）
     */
    @PostMapping("/tools/sql.run")
    public SqlResponse runSql(@RequestBody SqlRequest request) {
        String sql = request.getSql().trim().toUpperCase();

        // 安全检查：仅允许 SELECT 语句
        if (!sql.startsWith("SELECT")) {
            throw new IllegalArgumentException("仅支持 SELECT 查询，不允许执行 " + sql.split(" ")[0] + " 语句");
        }

        // Mock 返回结果（根据 SQL 关键词简单匹配）
        if (sql.contains("CUSTOMERS")) {
            return new SqlResponse(
                    List.of("id", "name", "email", "phone"),
                    List.of(
                            List.of(1, "张三", "zhangsan@example.com", "13800138000"),
                            List.of(2, "李四", "lisi@example.com", "13900139000"),
                            List.of(3, "王五", "wangwu@example.com", "13700137000")
                    ),
                    3
            );
        } else if (sql.contains("ORDERS")) {
            if (sql.contains("SUM")) {
                // SELECT SUM(total_amount) FROM orders
                return new SqlResponse(
                        List.of("SUM(total_amount)"),
                        List.of(List.of(12500.50)),
                        1
                );
            } else {
                return new SqlResponse(
                        List.of("id", "customer_id", "order_date", "total_amount", "status"),
                        List.of(
                                List.of(101, 1, "2024-01-15", 2500.00, "completed"),
                                List.of(102, 2, "2024-01-16", 3800.50, "shipped"),
                                List.of(103, 1, "2024-01-17", 6200.00, "paid")
                        ),
                        3
                );
            }
        } else if (sql.contains("ORDER_ITEMS")) {
            return new SqlResponse(
                    List.of("id", "order_id", "product_name", "quantity", "unit_price", "subtotal"),
                    List.of(
                            List.of(1, 101, "MacBook Pro", 1, 15000.00, 15000.00),
                            List.of(2, 101, "Magic Mouse", 2, 699.00, 1398.00),
                            List.of(3, 102, "iPhone 15", 1, 5999.00, 5999.00)
                    ),
                    3
            );
        } else {
            // 默认返回空结果
            return new SqlResponse(List.of(), List.of(), 0);
        }
    }

    /**
     * POST /mcp/tools/sql.validate - 验证 SQL（Mock 实现）
     */
    @PostMapping("/tools/sql.validate")
    public Map<String, Object> validateSql(@RequestBody SqlRequest request) {
        String sql = request.getSql().trim().toUpperCase();

        Map<String, Object> result = new HashMap<>();
        result.put("sql", request.getSql());

        if (!sql.startsWith("SELECT")) {
            result.put("valid", false);
            result.put("error", "仅支持 SELECT 查询");
        } else if (sql.contains("DROP") || sql.contains("DELETE") || sql.contains("UPDATE")) {
            result.put("valid", false);
            result.put("error", "SQL 包含危险操作关键字");
        } else {
            result.put("valid", true);
            result.put("message", "SQL 语法验证通过");
        }

        return result;
    }
}
