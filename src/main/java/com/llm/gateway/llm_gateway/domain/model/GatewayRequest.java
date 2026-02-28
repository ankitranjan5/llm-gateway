package com.llm.gateway.llm_gateway.domain.model;

import java.util.List;
import java.util.Map;

// This captures the incoming JSON perfectly
public record GatewayRequest(
        String model,                // We will hijack this for routing
        List<MessageDto> messages,
        Double temperature,
        Boolean stream,

        // CAPTURE EXTRA FIELDS:
        // If a client sends {"custom_strategy": "fast"}, it lands here!
        Map<String, Object> metadata
) {
    public record MessageDto(String role, String content) {}
}


