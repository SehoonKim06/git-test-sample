package com.twolinecloud.MCP_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    
    private static final String USER_AGENT = "MCP-Gateway/1.0";
    
    @Bean
    public RestClient restClient() {
        return RestClient.builder()
            .defaultHeader("User-Agent", USER_AGENT)
            .build();
    }
}