package com.example.demo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 诊断控制器 - 用于调试 API 连接问题
 */
@Slf4j
@RestController
@RequestMapping("/diagnostic")
@RequiredArgsConstructor
public class DiagnosticController {

    private final ChatModel chatModel;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    /**
     * 检查配置
     */
    @GetMapping("/config")
    public Map<String, String> checkConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("baseUrl", baseUrl);
        config.put("apiKey", maskApiKey(apiKey));
        config.put("model", model);
        config.put("chatModelClass", chatModel.getClass().getName());
        return config;
    }

    /**
     * 测试简单调用（不使用 Function）
     */
    @GetMapping("/test-simple")
    public Map<String, Object> testSimpleCall() {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("开始测试简单 LLM 调用...");
            log.info("Base URL: {}", baseUrl);
            log.info("Model: {}", model);
            log.info("API Key (masked): {}", maskApiKey(apiKey));

            String response = chatModel.call("你好，请回复'测试成功'");

            result.put("success", true);
            result.put("response", response);
            log.info("调用成功: {}", response);

        } catch (Exception e) {
            log.error("调用失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("errorClass", e.getClass().getName());

            // 提取更详细的错误信息
            Throwable cause = e.getCause();
            if (cause != null) {
                result.put("rootCause", cause.getMessage());
            }
        }

        return result;
    }

    private String maskApiKey(String key) {
        if (key == null || key.length() < 8) {
            return "***";
        }
        return key.substring(0, 7) + "***" + key.substring(key.length() - 4);
    }
}
