package com.example.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
public class McpClientConfig {

    @Value("${mcp.server.base-url}")
    private String mcpServerBaseUrl;

    @Bean
    public WebClient mcpWebClient() {
        log.info("=== 初始化 MCP WebClient ===");
        log.info("MCP Server Base URL: {}", mcpServerBaseUrl);
        
        return WebClient.builder()
                .baseUrl(mcpServerBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
