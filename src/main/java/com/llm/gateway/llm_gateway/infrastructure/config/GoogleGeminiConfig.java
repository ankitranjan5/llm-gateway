package com.llm.gateway.llm_gateway.infrastructure.config;
import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleGeminiConfig {

    @Value("${gemini.api.key}")
    String apiKey;

    @Bean("googleGeminiClient")
    public Client googleGeminiClient() {
        return Client.builder().apiKey(apiKey).build();
    }
}
