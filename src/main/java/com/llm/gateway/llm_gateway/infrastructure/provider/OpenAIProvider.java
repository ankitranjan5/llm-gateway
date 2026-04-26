package com.llm.gateway.llm_gateway.infrastructure.provider;

import com.openai.client.OpenAIClient;
import com.llm.gateway.llm_gateway.domain.model.GatewayRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service("openai")
public class OpenAIProvider extends OpenAITypeProvider{

    public OpenAIProvider(@Qualifier("openaiClient") OpenAIClient client) {
        super(client);
    }

    @Override
    @CircuitBreaker(name = "openai")
    public void streamChat(GatewayRequest request, Consumer<String> chunkHandler) {
        // 1. Internal Mapping (DTO -> OpenAI SDK)
        super.streamChat(request, chunkHandler);
    }

}