package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class McpClientConfig {

    @Value("${mcp.server.base-url}")
    private String mcpServerBaseUrl;

    @Bean
    public WebClient mcpWebClient() {
        return WebClient.builder()
                .baseUrl(mcpServerBaseUrl)
                .build();
    }
}
