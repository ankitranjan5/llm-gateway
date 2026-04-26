package com.llm.gateway.llm_gateway.infrastructure.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnthropicConfig {
    @Value("${anthropic.api.key}")
    private String apiKey;

    @Bean("anthropicClient")
    public AnthropicClient client(){
        return new AnthropicOkHttpClient.Builder()
                .apiKey(apiKey)
                .build();
    }
}
