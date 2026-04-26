package com.llm.gateway.llm_gateway.infrastructure.provider;

import com.llm.gateway.llm_gateway.domain.model.GatewayRequest;
import com.openai.client.OpenAIClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service("groq")
public class GroqProvider extends OpenAITypeProvider{
    public GroqProvider(@Qualifier("groqClient") OpenAIClient client) {
        super(client);
    }

    @Override
    @CircuitBreaker(name = "groq")
    public void streamChat(GatewayRequest request, Consumer<String> chunkHandler) {
        // 1. Internal Mapping (DTO -> OpenAI SDK)
        super.streamChat(request, chunkHandler);
    }
}
