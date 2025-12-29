package com.llm.gateway.llm_gateway.config;

import com.llm.gateway.llm_gateway.domain.port.LLMProvider;
import com.llm.gateway.llm_gateway.infrastructure.provider.OpenAIProvider;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIConfig {

    @Value("${openai.api.key}")
    private String apiKey;

    @Bean
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

    @Bean("openai") // Name this bean "openai"
    public LLMProvider standardOpenAIProvider(OpenAIClient openAIClient) {
        return new OpenAIProvider(openAIClient);
    }

    @Bean("groq")   // Name this bean "groq"
    public LLMProvider groqProvider(OpenAIClient groqClient) {
        // REUSE the same class, just inject the Groq client!
        return new OpenAIProvider(groqClient);
    }
}
