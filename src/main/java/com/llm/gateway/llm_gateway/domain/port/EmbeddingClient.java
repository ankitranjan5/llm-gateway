package com.llm.gateway.llm_gateway.domain.port;

import java.util.List;

public interface EmbeddingClient {
    /**
     * Converts text into a vector embedding.
     * @param text The input text to embed.
     * @return A list of doubles representing the vector (e.g., 1536 dimensions for OpenAI).
     */
    List<Float> embed(String text);
    int getDimensions();
}