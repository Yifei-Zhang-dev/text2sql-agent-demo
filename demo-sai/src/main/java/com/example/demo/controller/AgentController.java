package com.example.demo.controller;

import com.example.demo.dto.Text2SqlRequest;
import com.example.demo.dto.Text2SqlResponse;
import com.example.demo.service.Text2SqlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Agent 控制器 - Text-to-SQL 功能（Spring AI 版）
 * - 职责：接收 HTTP 请求，调用 Service，返回响应
 * - 不包含业务逻辑
 */
@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
@Tag(name = "Agent API", description = "智能代理接口，支持自然语言转 SQL 查询")
public class AgentController {

    private final Text2SqlService text2SqlService;

    /**
     * POST /agent/text2sql - 自然语言转 SQL 并执行
     */
    @Operation(summary = "自然语言转 SQL", description = "输入自然语言问题，自动生成 SQL 并执行，返回查询结果和解释")
    @PostMapping("/text2sql")
    public Text2SqlResponse text2Sql(@RequestBody Text2SqlRequest request) {
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
}
