package com.example.demosaia.controller;

import com.example.demosaia.dto.ScriptResponse;
import com.example.demosaia.dto.Text2SqlRequest;
import com.example.demosaia.service.Text2SqlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Agent 控制器 - Text-to-SQL 功能（Spring AI Alibaba 版）
 * - 职责：接收 HTTP 请求，调用 Service，返回响应
 * - 不包含业务逻辑
 */
@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
@Tag(name = "Agent API (Alibaba)", description = "智能代理接口，使用 Spring AI Alibaba 支持自然语言转 SQL 查询")
public class AgentController {

    private final Text2SqlService text2SqlService;

    /**
     * POST /agent/text2sql - 自然语言转 SQL 并生成 JavaScript 脚本
     */
    @Operation(summary = "自然语言转 SQL", description = "输入自然语言问题，自动生成 JavaScript 脚本，返回脚本代码和解释")
    @PostMapping("/text2sql")
    public ScriptResponse text2Sql(@RequestBody Text2SqlRequest request) {
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
