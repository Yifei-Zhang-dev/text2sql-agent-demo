package com.example.mcp.service;

import com.example.mcp.dto.SchemaResponse;
import com.example.mcp.dto.SqlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DatabaseService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取表结构信息
     */
    public SchemaResponse getTableSchema(String tableName) {
        // 验证表是否存在
        String checkTableSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = UPPER(?)";
        Integer count = jdbcTemplate.queryForObject(checkTableSql, Integer.class, tableName);

        if (count == null || count == 0) {
            throw new IllegalArgumentException("表 '" + tableName + "' 不存在，可用表：customers, orders, order_items");
        }

        // 查询列信息（H2 的 INFORMATION_SCHEMA）
        String columnSql = """
            SELECT COLUMN_NAME, TYPE_NAME, REMARKS
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_NAME = UPPER(?)
            ORDER BY ORDINAL_POSITION
            """;

        List<SchemaResponse.Column> columns = jdbcTemplate.query(columnSql, (rs, rowNum) -> {
            String name = rs.getString("COLUMN_NAME");
            String type = rs.getString("TYPE_NAME");
            String comment = rs.getString("REMARKS");
            return new SchemaResponse.Column(name, type, comment != null ? comment : "");
        }, tableName);

        return new SchemaResponse(tableName, columns);
    }

    /**
     * 执行只读 SQL 查询
     */
    public SqlResponse executeSql(String sql) {
        // 安全检查：仅允许 SELECT
        String trimmedSql = sql.trim().toUpperCase();
        if (!trimmedSql.startsWith("SELECT")) {
            throw new IllegalArgumentException("仅支持 SELECT 查询，不允许执行 " + trimmedSql.split(" ")[0] + " 语句");
        }

        // 执行查询
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql);
        SqlRowSetMetaData metaData = rowSet.getMetaData();

        // 提取列名
        List<String> columns = new ArrayList<>();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            columns.add(metaData.getColumnLabel(i));
        }

        // 提取行数据
        List<List<Object>> rows = new ArrayList<>();
        while (rowSet.next()) {
            List<Object> row = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                row.add(rowSet.getObject(i));
            }
            rows.add(row);
        }

        return new SqlResponse(columns, rows, rows.size());
    }

    /**
     * 验证 SQL 语法（简单实现）
     */
    public boolean validateSql(String sql) {
        String trimmedSql = sql.trim().toUpperCase();

        // 检查是否是 SELECT
        if (!trimmedSql.startsWith("SELECT")) {
            return false;
        }

        // 检查危险关键字
        String[] dangerousKeywords = {"DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "CREATE", "TRUNCATE"};
        for (String keyword : dangerousKeywords) {
            if (trimmedSql.contains(keyword)) {
                return false;
            }
        }

        return true;
    }
}
