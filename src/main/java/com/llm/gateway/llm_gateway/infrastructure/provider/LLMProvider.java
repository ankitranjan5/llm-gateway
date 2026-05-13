package com.llm.gateway.llm_gateway.infrastructure.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.llm.gateway.llm_gateway.domain.model.GatewayRequest;

import java.util.function.Consumer;

public interface LLMProvider {
    /**
     * Streams the response token-by-token.
     * @param request The unified request object
     * @param chunkHandler A callback that handles each text chunk as it arrives
     */
    void streamChat(GatewayRequest request, Consumer<String> chunkHandler);

    /**
     * Extended entry point for providers that surface structured tool output on non-streaming completions.
     * Default implementation ignores {@code toolCallsConsumer} aside from signaling {@code null} when invoked.
     */
    default void streamChat(
            GatewayRequest request,
            Consumer<String> chunkHandler,
            Consumer<JsonNode> toolCallsConsumer) {
        streamChat(request, chunkHandler);
        if (toolCallsConsumer != null) {
            toolCallsConsumer.accept(null);
        }
    }
}
