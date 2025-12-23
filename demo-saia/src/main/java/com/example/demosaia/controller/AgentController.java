package com.example.demosaia.controller;

import com.example.demosaia.dto.Text2SqlRequest;
import com.example.demosaia.dto.Text2SqlResponse;
import com.example.demosaia.service.McpToolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.*;

/**
 * Agent 控制器 - Text-to-SQL 功能（Spring AI Alibaba 版本）
 */
@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
@Tag(name = "Agent API (Alibaba)", description = "智能代理接口，使用 Spring AI Alibaba 支持自然语言转 SQL 查询")
public class AgentController {

    private final ChatModel chatModel;

    /**
     * POST /agent/text2sql - 自然语言转 SQL 并执行
     */
    @Operation(summary = "自然语言转 SQL", description = "输入自然语言问题，自动生成 SQL 并执行，返回查询结果和解释")
    @PostMapping("/text2sql")
    public Text2SqlResponse text2Sql(@RequestBody Text2SqlRequest request) {
        log.info("\n========== Text-to-SQL 请求开始 (Spring AI Alibaba) ==========");
        log.info("用户问题: {}", request.getQuestion());

        // 优化的 System Prompt
        String systemPrompt = """
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

        try {
            log.info("正在调用 LLM (Spring AI Alibaba)...");

            // 使用 Spring AI Alibaba ChatClient 进行 Function Calling
            // Note: Spring AI 1.0.0 uses toolNames() instead of functions()
            ChatResponse chatResponse = ChatClient.create(chatModel)
                    .prompt()
                    .system(systemPrompt)
                    .user(request.getQuestion())
                    .toolNames("schemaGet", "sqlRun")
                    .call()
                    .chatResponse();

            String response = chatResponse.getResult().getOutput().getText();

            // 从 McpToolService 获取最后执行的 SQL
            String executedSql = McpToolService.getLastExecutedSql();

            log.info("LLM 最终响应: {}", response);
            log.info("提取的 SQL: {}", executedSql);
            log.info("Token 使用情况: {}", chatResponse.getMetadata().getUsage());
            log.info("========== Text-to-SQL 请求完成 ==========\n");

            // 返回结构化响应
            return new Text2SqlResponse(
                    executedSql != null ? executedSql : "（未检测到 SQL）",
                    response,
                    "查询完成"
            );

        } catch (Exception e) {
            log.error("========== Text-to-SQL 执行失败 ==========", e);
            return new Text2SqlResponse(
                    null,
                    "执行失败: " + e.getMessage(),
                    "请检查问题描述、MCP Server 状态或 LLM 配置"
            );
        }
    }

    /**
     * POST /agent/chat - 通用对话接口（可选）
     */
    @Operation(summary = "通用对话", description = "与 LLM 进行通用对话（不使用工具）")
    @PostMapping("/chat")
    public String chat(@RequestBody String message) {
        log.info("收到对话请求: {}", message);

        try {
            return ChatClient.create(chatModel)
                    .prompt()
                    .user(message)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("对话失败", e);
            return "对话失败: " + e.getMessage();
        }
    }
}
