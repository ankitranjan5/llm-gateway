package com.llm.gateway.llm_gateway.api;

public record NonStreamingChatResponse(
        String content,
        String provider,
        String modelRequested,
        String modelUsed
) {}
