package com.llm.gateway.llm_gateway.application.service;

import com.llm.gateway.llm_gateway.domain.model.ChatRequest;
import com.llm.gateway.llm_gateway.domain.port.LLMProvider;
import com.llm.gateway.llm_gateway.dto.ExecutionMetadata;
import com.llm.gateway.llm_gateway.dto.GatewayRequest;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Service
public class RouterService {

    private static final Logger log = LoggerFactory.getLogger(RouterService.class);
    private final Map<String, LLMProvider> providers;

    // Spring injects all implementations of LLMProvider into a Map
    // Key = Bean Name (e.g., "openAIProvider", "mockProvider")
    public RouterService(Map<String, LLMProvider> providers) {
        this.providers = providers;
    }

    public ExecutionMetadata routeAndExecute(GatewayRequest request, Consumer<String> chunkHandler) {
        String targetModel = request.model();
        LLMProvider primary = getProvider(targetModel);

        try {
            // Attempt 1: Primary Provider
            log.info("Routing to primary provider for model: {}", targetModel);
            primary.streamChat(request, chunkHandler);
            return new ExecutionMetadata("openai", targetModel);

        } catch (CallNotPermittedException e) {
            // Case A: Circuit is OPEN (OpenAI is known to be down)
            log.warn("Circuit Open for {}. Failing over to Groq.", targetModel);
            return executeFallback(request, chunkHandler);

        }
        catch (Exception e) {
            // Case C: Unknown Error (500s, Network, etc.)
            log.error("Unexpected error from {}. Failing over.", targetModel, e);
            return executeFallback(request, chunkHandler);
        }
    }

    private LLMProvider getProvider(String model) {
        // Basic routing logic
        if (model.startsWith("llama")) return providers.get("groq");
        return providers.get("openai");
    }

    private ExecutionMetadata executeFallback(GatewayRequest request, Consumer<String> chunkHandler) {
        // FALLBACK STRATEGY:
        // If OpenAI (GPT-4) fails, we degrade gracefully to Llama 3 (via Groq).
        // It's faster, cheaper, and ensures the user gets *some* answer.

        String fallbackModel = "llama-3.3-70b-versatile";

        // 2. Create a NEW Request Object (Copy everything, change model)
        GatewayRequest fallbackRequest = new GatewayRequest(
                fallbackModel,
                request.messages(),
                request.temperature(),
                request.stream(),
                request.metadata()
        );

        log.info(">>> ENGAGING FALLBACK: Using Llama 3 (Groq) <<<");

        LLMProvider fallback = providers.get("groq");

        // Notify the user (Optional but good UX)
        // chunkHandler.accept("[System Notice: Primary model busy. Switching to backup...]\n\n");

        fallback.streamChat(fallbackRequest, chunkHandler);
        return new ExecutionMetadata("groq", fallbackModel);
    }
}
