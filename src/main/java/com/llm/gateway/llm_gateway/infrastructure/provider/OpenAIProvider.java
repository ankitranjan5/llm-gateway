package com.llm.gateway.llm_gateway.infrastructure.provider;

import com.llm.gateway.llm_gateway.application.service.RouterService;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.ChatModel;
import com.llm.gateway.llm_gateway.dto.GatewayRequest;
import com.llm.gateway.llm_gateway.domain.port.LLMProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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