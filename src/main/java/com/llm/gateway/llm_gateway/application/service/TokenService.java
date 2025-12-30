package com.llm.gateway.llm_gateway.application.service;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.llm.gateway.llm_gateway.dto.GatewayRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TokenService {

    private final EncodingRegistry registry;
    private final Encoding encoding;

    public TokenService() {
        this.registry = Encodings.newDefaultEncodingRegistry();
        // CL100K_BASE is the standard for GPT-3.5/4, Llama 3, etc.
        this.encoding = registry.getEncoding(EncodingType.CL100K_BASE);
    }

    // accurate count for a single string
    public int count(String text) {
        if (text == null || text.isEmpty()) return 0;
        return encoding.countTokens(text);
    }

    // Estimate input tokens from the request object
    // Note: This is an approximation. Real OpenAI billing adds ~3 tokens per message for formatting.
    public int estimateInputTokens(GatewayRequest request) {
        int total = 0;
        if (request.messages() != null) {
            for (var msg : request.messages()) {
                total += count(msg.content());
                // Add fixed overhead per message (role, formatting)
                total += 4;
            }
        }
        return total + 3; // Reply prime
    }
}
