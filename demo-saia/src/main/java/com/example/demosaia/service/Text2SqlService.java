package com.example.demosaia.service;

import com.example.demosaia.dto.Text2SqlRequest;
import com.example.demosaia.dto.Text2SqlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

/**
 * Text2SQL 业务逻辑服务
 * - 核心流程：接收自然语言问题 → 调用 LLM + Tools → 返回 SQL 和结果
 * - 使用 Spring AI 1.0.0 GA API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Text2SqlService {

    private final ChatClient chatClient;

    /**
     * System Prompt（指导 LLM 如何使用工具）
     */
    private static final String SYSTEM_PROMPT = """
            你是一个专业的数据库查询助手。请严格按照以下步骤操作：

            步骤 1: 分析用户问题，确定需要查询哪些表
            步骤 2: 调用 schemaGet 工具获取表结构（可多次调用）
            步骤 3: 根据表结构生成正确的 SQL（仅允许 SELECT 语句）
            步骤 4: 调用 sqlRun 工具执行 SQL
            步骤 5: 用中文简短解释查询结果（1-2 句话）

            可用表：
            - customers（客户表）：客户信息
            - orders（订单表）：订单记录
            - order_items（订单项表）：订单明细

            注意事项：
            - 必须先调用 schemaGet 再生成 SQL
            - SQL 必须基于实际字段名（大小写敏感）
            - 关联查询需要正确使用 JOIN
            - 最后返回简短的中文解释
            """;

    /**
     * 执行 Text-to-SQL 流程
     *
     * @param request 用户请求（包含自然语言问题）
     * @return SQL、结果解释和状态
     */
    public Text2SqlResponse executeText2Sql(Text2SqlRequest request) {
        log.info("\n========== Text-to-SQL 请求开始 (Spring AI Alibaba) ==========");
        log.info("用户问题: {}", request.getQuestion());

        try {
            log.info("正在调用 LLM (Spring AI Alibaba)...");

            // 使用 Spring AI 1.0.0 GA API（与 Spring AI 保持一致）
            ChatResponse chatResponse = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(request.getQuestion())
                    .toolNames("schemaGet", "sqlRun")  // Spring AI 1.0.0 GA tool calling API
                    .call()
                    .chatResponse();

            // 获取 LLM 响应（使用 Spring AI 1.0.0 标准 API）
            String llmAnswer = chatResponse.getResult().getOutput().getText();

            // 从 McpToolService 获取最后执行的 SQL 和查询结果
            String executedSql = McpToolService.getLastExecutedSql();
            com.example.demosaia.dto.QueryResult queryResult = McpToolService.getLastQueryResult();

            log.info("LLM 最终响应: {}", llmAnswer);
            log.info("提取的 SQL: {}", executedSql);
            if (queryResult != null) {
                log.info("查询结果: {} 行, {} 列", queryResult.getRowCount(), queryResult.getColumns().size());
            }
            log.info("Token 使用情况: {}", chatResponse.getMetadata().getUsage());
            log.info("========== Text-to-SQL 请求完成 ==========\n");

            // 返回结构化响应（包含 SQL、结构化结果、LLM 解释）
            return new Text2SqlResponse(
                    executedSql != null ? executedSql : "（未检测到 SQL）",
                    queryResult,
                    llmAnswer
            );

        } catch (Exception e) {
            log.error("========== Text-to-SQL 执行失败 ==========", e);
            return new Text2SqlResponse(
                    "执行失败",
                    null,
                    "执行失败: " + e.getMessage() + "\n请检查问题描述、MCP Server 状态或 LLM 配置"
            );
        }
    }

    /**
     * 通用对话接口（不使用工具）
     *
     * @param message 用户消息
     * @return LLM 响应
     */
    public String chat(String message) {
        log.info("收到对话请求: {}", message);

        try {
            return chatClient.prompt()
                    .user(message)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("对话失败", e);
            return "对话失败: " + e.getMessage();
        }
    }
}
