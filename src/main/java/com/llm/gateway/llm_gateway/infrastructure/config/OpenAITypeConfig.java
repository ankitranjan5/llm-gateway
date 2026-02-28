package com.llm.gateway.llm_gateway.infrastructure.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAITypeConfig {

    @Value("${openai.api.key}")
    private String apiKey;

    @Bean("openaiClient")
    public OpenAIClient openAIClient() {
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    @Bean("groqClient")
    public OpenAIClient groqClient(@Value("${groq.api.key}") String key) {
        return OpenAIOkHttpClient.builder()
                .apiKey(key)
                .baseUrl("https://api.groq.com/openai/v1") // <--- The override
                .build();
    }

}
