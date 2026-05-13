package com.llm.gateway.llm_gateway.api;

import com.fasterxml.jackson.databind.JsonNode;

public record NonStreamingChatResponse(
        String content,
        String provider,
        String modelRequested,
        String modelUsed,
        JsonNode toolCalls
) {}
