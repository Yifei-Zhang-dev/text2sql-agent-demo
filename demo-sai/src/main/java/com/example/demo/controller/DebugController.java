package com.example.demo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 调试控制器 - 查看完整配置
 */
@Slf4j
@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
public class DebugController {

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    @GetMapping("/full-config")
    public Map<String, String> getFullConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("baseUrl", baseUrl);
        config.put("apiKey", maskApiKey(apiKey));
        config.put("model", model);
        config.put("expectedChatUrl", baseUrl + "/chat/completions");
        config.put("note", "如果 expectedChatUrl 不正确，需要调整 base-url");
        return config;
    }

    private String maskApiKey(String key) {
        if (key == null || key.length() < 12) {
            return "***";
        }
        return key.substring(0, 7) + "***" + key.substring(key.length() - 4);
    }
}
