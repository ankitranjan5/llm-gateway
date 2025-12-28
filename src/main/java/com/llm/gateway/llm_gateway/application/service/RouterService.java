package com.llm.gateway.llm_gateway.application.service;

import com.llm.gateway.llm_gateway.domain.model.ChatRequest;
import com.llm.gateway.llm_gateway.domain.port.LLMProvider;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class RouterService {

    private final Map<String, LLMProvider> providers;

    // Spring injects all implementations of LLMProvider into a Map
    // Key = Bean Name (e.g., "openAIProvider", "mockProvider")
    public RouterService(Map<String, LLMProvider> providers) {
        this.providers = providers;
    }

    public LLMProvider route(String model) {
        // Simple Logic: Check generic model name
        if (model.startsWith("gpt")) {
            return providers.get("openAIProvider");
        }
        // Fallback or Test Mode
        return providers.get("mockProvider");
    }
}
