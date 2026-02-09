package com.example.demosaia.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.example.demosaia.graph.state.Text2SqlState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renderer Node - 脚本渲染节点
 * 职责：根据问题和 SQL 生成 JavaScript 脚本和选择组件类型
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RendererNode implements Function<OverAllState, Map<String, Object>> {

    private final ChatClient chatClient;

    private static final String RENDERER_PROMPT = """
            你是数据可视化专家。根据用户问题生成 JavaScript 脚本。

            【任务】
            1. 判断最合适的组件类型：Table/PieChart/LineChart/BarChart/DataPoint
            2. 生成 async function generateData(mcpClient) {...}
            3. 返回格式：{ componentType: '类型', propertyData: {...} }

            【组件类型】
            - Table：列表查询、聚合查询
            - PieChart：占比、分布（如"订单状态分布"）
            - LineChart：趋势、时间序列（如"每月订单量"）
            - BarChart：对比、排行（如"各城市客户数"）
            - DataPoint：单值统计（COUNT/SUM/AVG）

            【已生成的 SQL】
            {sql}

            【用户问题】
            {question}

            【要求】
            - 必须用 ```javascript 代码块包裹
            - 使用 mcpClient.executeSql(sql) 执行 SQL
            - 根据查询类型选择合适的组件
            """;

    @Override
    public Map<String, Object> apply(OverAllState overAllState) {
        Text2SqlState state = Text2SqlState.fromMap(overAllState.data());

        log.info("[RendererNode] 开始生成脚本");
        state.addLog("[RendererNode] 开始生成脚本");

        try {
            String sql = state.getValidatedSql() != null ? state.getValidatedSql() : state.getSql();

            // 调用 LLM 生成脚本
            String llmResponse = chatClient.prompt()
                    .user(userSpec -> userSpec.text(
                            RENDERER_PROMPT
                                    .replace("{sql}", sql)
                                    .replace("{question}", state.getQuestion())
                    ))
                    .call()
                    .content();

            // 提取 JavaScript 代码
            String scriptCode = extractScriptCode(llmResponse);

            state.setScriptCode(scriptCode);
            state.setExplanation("基于 Graph 编排生成的可视化脚本");
            state.addLog("[RendererNode] 脚本生成完成");

            log.info("[RendererNode] 脚本生成完成");

            return state.toMap();

        } catch (Exception e) {
            log.error("[RendererNode] 脚本生成失败", e);
            state.addLog("[RendererNode] 失败: " + e.getMessage());

            state.setScriptCode(generateErrorScript(e.getMessage()));
            state.setExplanation("脚本生成失败：" + e.getMessage());

            return state.toMap();
        }
    }

    /**
     * 从 LLM 响应中提取 JavaScript 代码
     */
    private String extractScriptCode(String llmResponse) {
        Pattern pattern = Pattern.compile("```javascript\\s*([\\s\\S]*?)```");
        Matcher matcher = pattern.matcher(llmResponse);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 如果没有代码块，返回整个响应
        log.warn("[RendererNode] 未找到 JavaScript 代码块，返回原始响应");
        return llmResponse;
    }

    /**
     * 生成错误提示脚本
     */
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
                """.formatted(errorMessage);
    }
}
