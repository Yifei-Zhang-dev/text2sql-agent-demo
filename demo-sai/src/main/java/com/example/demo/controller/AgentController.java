package com.example.demo.controller;

import com.example.demo.dto.Text2SqlRequest;
import com.example.demo.dto.Text2SqlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.*;

/**
 * Agent 控制器 - Text-to-SQL 功能
 */
@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final ChatModel chatModel;

    /**
     * POST /agent/text2sql - 自然语言转 SQL 并执行
     */
    @PostMapping("/text2sql")
    public Text2SqlResponse text2Sql(@RequestBody Text2SqlRequest request) {
        log.info("收到 Text-to-SQL 请求: {}", request.getQuestion());

        // 构建 System Prompt
        String systemPrompt = """
            你是一个数据库助手。用户会用自然语言提问，你需要：
            1. 先调用 schemaGet 工具获取相关表的结构
            2. 根据表结构生成正确的 SQL 查询（仅 SELECT）
            3. 调用 sqlRun 工具执行 SQL
            4. 用中文简短解释查询结果

            可用表：customers（客户）、orders（订单）、order_items（订单项）
            """;

        try {
            // 使用 Spring AI ChatClient 进行 Function Calling
            String response = ChatClient.create(chatModel)
                    .prompt()
                    .system(systemPrompt)
                    .user(request.getQuestion())
                    .functions("schemaGet", "sqlRun")  // 注册可用函数
                    .call()
                    .content();

            log.info("Agent 响应: {}", response);

            // 简化返回（实际应解析 LLM 响应提取 SQL 和结果）
            return new Text2SqlResponse(
                    "（由 LLM 生成）",
                    response,
                    "查询完成"
            );

        } catch (Exception e) {
            log.error("Text-to-SQL 执行失败", e);
            return new Text2SqlResponse(
                    null,
                    "执行失败: " + e.getMessage(),
                    "请检查问题描述或 LLM 配置"
            );
        }
    }

    /**
     * POST /agent/chat - 通用对话接口（可选）
     */
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
