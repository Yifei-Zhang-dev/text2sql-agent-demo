package com.example.demosaia.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.example.demosaia.dto.ScriptResponse;
import com.example.demosaia.dto.Text2SqlRequest;
import com.example.demosaia.graph.state.Text2SqlState;
import com.example.demosaia.service.Text2SqlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Agent 控制器 - Text-to-SQL 功能
 * 提供两个版本的 Text-to-SQL 接口：
 * 1. /text2sql - 单 Agent 版本（原有实现）
 * 2. /text2sql-graph - Graph 多 Agent 版本（新增实现）
 */
@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
@Tag(name = "Agent API", description = "Text-to-SQL Agent 接口")
public class AgentController {

    private final Text2SqlService text2SqlService;
    private final StateGraph text2SqlGraph;

    /**
     * POST /agent/text2sql - 单 Agent 版本（原有实现）
     */
    @Operation(summary = "Text-to-SQL 查询（单 Agent 版本）",
               description = "使用单个 Agent 处理自然语言到 SQL 的转换")
    @PostMapping("/text2sql")
    public ScriptResponse text2Sql(@RequestBody Text2SqlRequest request) {
        log.info("[单Agent] 收到查询: {}", request.getQuestion());
        return text2SqlService.executeText2Sql(request);
    }

    /**
     * POST /agent/chat - 通用对话接口（可选）
     */
    @Operation(summary = "通用对话", description = "与 LLM 进行通用对话（不使用工具）")
    @PostMapping("/chat")
    public String chat(@RequestBody String message) {
        return text2SqlService.chat(message);
    }

    /**
     * POST /agent/text2sql-graph - Graph 多 Agent 版本
     */
    @PostMapping("/text2sql-graph")
    @Operation(summary = "Text-to-SQL 查询（Graph 版本）",
               description = "使用多 Agent Graph 编排处理自然语言到 SQL 的转换")
    public ScriptResponse text2SqlGraph(@RequestBody Text2SqlRequest request) {
        log.info("[Graph] 收到查询: {}", request.getQuestion());

        try {
            // 1. 准备初始状态
            Text2SqlState initialState = new Text2SqlState();
            initialState.setQuestion(request.getQuestion());
            initialState.addLog("[Controller] 开始 Graph 执行");

            // 2. 编译并执行 Graph
            log.info("[Graph] 开始编译 Graph...");
            CompiledGraph compiledGraph = text2SqlGraph.compile();

            log.info("[Graph] 开始执行 Graph...");
            OverAllState finalState = compiledGraph.invoke(initialState.toMap())
                    .orElseThrow(() -> new RuntimeException("Graph 执行返回空结果"));

            // 4. 提取结果
            Text2SqlState resultState = Text2SqlState.fromMap(finalState.data());

            log.info("[Graph] 执行完成");
            log.debug("[Graph] 执行日志:\n{}", resultState.getExecutionLog());

            // 5. 构建响应
            ScriptResponse.ScriptResponseBuilder builder = ScriptResponse.builder()
                    .scriptCode(resultState.getScriptCode())
                    .explanation(resultState.getExplanation())
                    .executionLog(resultState.getExecutionLog().toString());

            // 如果有错误信息，附加到响应
            if (resultState.getErrorNode() != null) {
                builder.errorInfo(ScriptResponse.ErrorInfo.builder()
                        .failedNode(resultState.getErrorNode())
                        .errorType(resultState.getErrorType())
                        .errorDetail(resultState.getErrorDetail())
                        .suggestion(resultState.getErrorSuggestion())
                        .retryable(Boolean.TRUE.equals(resultState.getErrorRetryable()))
                        .build());
            }

            return builder.build();

        } catch (Exception e) {
            log.error("[Graph] 执行失败", e);

            return ScriptResponse.builder()
                    .scriptCode(generateErrorScript(e.getMessage()))
                    .explanation("Graph 执行失败: " + e.getMessage())
                    .errorInfo(ScriptResponse.ErrorInfo.builder()
                            .failedNode("GraphEngine")
                            .errorType(classifyTopLevelError(e))
                            .errorDetail(e.getMessage())
                            .suggestion("服务暂时不可用，请稍后重试。")
                            .retryable(true)
                            .build())
                    .build();
        }
    }

    private String classifyTopLevelError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (msg.contains("timeout") || msg.contains("connect")) return "NETWORK_ERROR";
        return "UNKNOWN";
    }

    private String generateErrorScript(String errorMessage) {
        return """
                async function generateData(mcpClient) {
                    return {
                        componentType: 'DataPoint',
                        propertyData: {
                            value: 'ERROR',
                            label: '%s'
                        }
                    };
                }
                """.formatted(errorMessage.replace("'", "\\'"));
    }
}
