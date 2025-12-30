package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 查询结果数据结构
 * - 封装 MCP sql.run 的返回结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryResult {
    private List<String> columns;
    private List<List<Object>> rows;
    private int rowCount;
}
