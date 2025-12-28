package com.example.demosaia.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatClient 配置类
 * - 统一创建 ChatClient Bean
 * - Spring AI Alibaba 会自动使用 application.yml 中配置的 options
 */
@Slf4j
@Configuration
public class ChatClientConfig {

    /**
     * 创建 ChatClient Bean
     * 注意：默认 options 由 Spring AI Alibaba Auto-configuration 从 application.yml 读取
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        log.info("初始化 ChatClient (Spring AI Alibaba)");
        return ChatClient.builder(chatModel).build();
    }
}
