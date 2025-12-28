package com.llm.gateway.llm_gateway.infrastructure.provider;

import com.llm.gateway.llm_gateway.domain.model.ChatRequest;
import com.llm.gateway.llm_gateway.domain.port.LLMProvider;
import com.llm.gateway.llm_gateway.dto.GatewayRequest;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service("mockProvider")
public class MockProvider implements LLMProvider {
    @Override
    public void streamChat(GatewayRequest request, Consumer<String> chunkHandler) {
        String[] words = {"This ", "is ", "a ", "mock ", "response ", "from ", "Java ", "21!"};

        for (String word : words) {
            try {
                Thread.sleep(200); // Simulate network latency
                chunkHandler.accept(word);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
