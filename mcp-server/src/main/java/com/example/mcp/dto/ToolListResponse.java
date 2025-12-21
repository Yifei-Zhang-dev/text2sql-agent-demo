package com.example.mcp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolListResponse {
    private String version;
    private List<Tool> tools;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tool {
        private String name;
        private String description;
        private InputSchema inputSchema;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InputSchema {
        private String type;
        private List<String> required;
        private Object properties;
    }
}
