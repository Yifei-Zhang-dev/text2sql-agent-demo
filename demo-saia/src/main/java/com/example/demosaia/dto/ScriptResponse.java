package com.example.demosaia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 返回结构（新方案）
 * 后端返回LLM生成的JavaScript脚本，由前端执行。
 * 工作流程：
 *   后端调用LLM生成JavaScript脚本
 *   后端返回 scriptCode + explanation
 *   前端执行脚本，得到 componentType 和 propertyData
 *   前端根据组件类型动态渲染
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptResponse {

    /**
     * LLM生成的JavaScript脚本代码
     * 脚本是一个async函数，接收mcpClient参数，返回{componentType, propertyData}
     */
    private String scriptCode;

    /**
     * 中文解释
     * LLM对查询结果的简短说明（1-2句话）
     */
    private String explanation;
}
