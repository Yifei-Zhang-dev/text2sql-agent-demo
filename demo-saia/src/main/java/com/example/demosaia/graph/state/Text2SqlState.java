package com.example.demosaia.graph.state;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Text2SQL Graph 的状态类
 * 用于在各个 Node 之间传递数据
 */
@Data
public class Text2SqlState {

    // === 输入数据 ===
    /**
     * 用户的自然语言问题
     */
    private String question;

    /**
     * 分页参数（可选）：cursor（列表查询）或 page（聚合查询）
     */
    private String paginationParam;

    // === Router Node 输出 ===
    /**
     * 查询类型：simple（简单查询） 或 complex（复杂查询）
     */
    private String queryType;

    // === Schema Retrieval Node 输出 ===
    /**
     * 数据库表结构信息
     */
    private Map<String, Object> schema;

    // === SQL Generator Nodes 输出 ===
    /**
     * 生成的 SQL 语句
     */
    private String sql;

    // === SQL Validator Node 输出 ===
    /**
     * 验证后的 SQL 语句
     */
    private String validatedSql;

    /**
     * SQL 是否通过验证
     */
    private Boolean isValid;

    /**
     * 验证失败的错误信息（如果有）
     */
    private String validationError;

    // === Renderer Node 输出 ===
    /**
     * 生成的 JavaScript 脚本代码
     */
    private String scriptCode;

    /**
     * 中文解释说明
     */
    private String explanation;

    /**
     * 选择的组件类型（Table/PieChart/LineChart/BarChart/DataPoint）
     */
    private String componentType;

    // === 辅助字段 ===
    /**
     * 执行过程中的日志信息（用于调试）
     */
    private StringBuilder executionLog = new StringBuilder();

    /**
     * 添加执行日志
     */
    public void addLog(String message) {
        executionLog.append("[").append(System.currentTimeMillis()).append("] ")
                .append(message).append("\n");
    }

    /**
     * 转换为 Map（用于 OverAllState）
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("question", question);
        map.put("paginationParam", paginationParam);
        map.put("queryType", queryType);
        map.put("schema", schema);
        map.put("sql", sql);
        map.put("validatedSql", validatedSql);
        map.put("isValid", isValid);
        map.put("validationError", validationError);
        map.put("scriptCode", scriptCode);
        map.put("explanation", explanation);
        map.put("componentType", componentType);
        map.put("executionLog", executionLog.toString());
        return map;
    }

    /**
     * 从 Map 构建 State（用于从 OverAllState 读取）
     */
    @SuppressWarnings("unchecked")
    public static Text2SqlState fromMap(Map<String, Object> map) {
        Text2SqlState state = new Text2SqlState();
        state.setQuestion((String) map.get("question"));
        state.setPaginationParam((String) map.get("paginationParam"));
        state.setQueryType((String) map.get("queryType"));
        state.setSchema((Map<String, Object>) map.get("schema"));
        state.setSql((String) map.get("sql"));
        state.setValidatedSql((String) map.get("validatedSql"));
        state.setIsValid((Boolean) map.get("isValid"));
        state.setValidationError((String) map.get("validationError"));
        state.setScriptCode((String) map.get("scriptCode"));
        state.setExplanation((String) map.get("explanation"));
        state.setComponentType((String) map.get("componentType"));

        String log = (String) map.get("executionLog");
        if (log != null) {
            state.executionLog = new StringBuilder(log);
        }
        return state;
    }
}
