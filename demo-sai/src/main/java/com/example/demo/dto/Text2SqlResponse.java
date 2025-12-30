package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Text2SqlResponse {
    private String sql;              // 执行的 SQL
    private QueryResult result;      // 结构化查询结果（columns, rows, rowCount）
    private String explanation;      // LLM 的自然语言解释
}
