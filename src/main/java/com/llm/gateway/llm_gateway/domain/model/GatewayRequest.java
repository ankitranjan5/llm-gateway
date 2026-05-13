package com.llm.gateway.llm_gateway.domain.model;

import java.util.List;
import java.util.Map;

// This captures the incoming JSON perfectly
public record GatewayRequest(
        String model,
        List<MessageDto> messages,
        Double temperature,
        Boolean stream,

        // CAPTURE EXTRA FIELDS:
        // If a client sends {"custom_strategy": "fast"}, it lands here!
        Map<String, Object> metadata,
        List<Map<String, Object>> tools,
        String tool_choice
) {
    public record MessageDto(String role, String content) {}

    /**
     * {@code metadata.streaming} overrides the top-level {@code stream} flag when present.
     * Defaults to streaming when neither is set.
     */
    public boolean shouldStream() {
        if (metadata != null && metadata.containsKey("streaming")) {
            return coerceStreamingFlag(metadata.get("streaming"));
        }
        if (stream != null) {
            return stream;
        }
        return true;
    }

    private static boolean coerceStreamingFlag(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return !"false".equalsIgnoreCase(s) && !"0".equals(s);
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        return true;
    }
}


