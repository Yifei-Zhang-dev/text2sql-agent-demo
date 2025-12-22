package com.example.mcp.service;

import com.example.mcp.dto.SchemaResponse;
import com.example.mcp.dto.SqlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取表结构信息
     */
    public SchemaResponse getTableSchema(String tableName) {
        log.info("开始查询表结构: {}", tableName);
        
        try {
            // 验证表是否存在
            String checkTableSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = UPPER(?)";
            log.info("执行验证 SQL: {}, 参数: {}", checkTableSql, tableName);
            Integer count = jdbcTemplate.queryForObject(checkTableSql, Integer.class, tableName);
            log.info("表存在检查结果: {}", count);

            if (count == null || count == 0) {
                throw new IllegalArgumentException("表 '" + tableName + "' 不存在，可用表：customers, orders, order_items");
            }

            // 查询列信息（H2 2.x 使用 DATA_TYPE 而非 TYPE_NAME）
            String columnSql = """
                SELECT COLUMN_NAME, DATA_TYPE, REMARKS
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = UPPER(?)
                ORDER BY ORDINAL_POSITION
                """;
            
            log.info("执行列查询 SQL: {}, 参数: {}", columnSql, tableName);
            List<SchemaResponse.Column> columns = jdbcTemplate.query(columnSql, (rs, rowNum) -> {
                String name = rs.getString("COLUMN_NAME");
                String type = rs.getString("DATA_TYPE");
                String comment = rs.getString("REMARKS");
                log.debug("查询到列: name={}, type={}, comment={}", name, type, comment);
                return new SchemaResponse.Column(name, type, comment != null ? comment : "");
            }, tableName);

            log.info("成功查询到 {} 列", columns.size());
            SchemaResponse response = new SchemaResponse(tableName, columns);
            log.info("返回响应: {}", response);
            return response;
        } catch (Exception e) {
            log.error("查询表结构失败: tableName={}", tableName, e);
            throw e;
        }
    }

    /**
     * 执行只读 SQL 查询
     */
    public SqlResponse executeSql(String sql) {
        log.info("开始执行 SQL: {}", sql);
        
        try {
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
            log.info("查询列: {}", columns);

            // 提取行数据
            List<List<Object>> rows = new ArrayList<>();
            while (rowSet.next()) {
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rowSet.getObject(i));
                }
                rows.add(row);
            }

            log.info("成功执行 SQL，返回 {} 行", rows.size());
            return new SqlResponse(columns, rows, rows.size());
        } catch (Exception e) {
            log.error("执行 SQL 失败: {}", sql, e);
            throw e;
        }
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
