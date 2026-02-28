package com.llm.gateway.llm_gateway.infrastructure.provider;

import com.llm.gateway.llm_gateway.domain.model.GatewayRequest;

import java.util.function.Consumer;

public interface LLMProvider {
    /**
     * Streams the response token-by-token.
     * @param request The unified request object
     * @param chunkHandler A callback that handles each text chunk as it arrives
     */
    void streamChat(GatewayRequest request, Consumer<String> chunkHandler);
}
