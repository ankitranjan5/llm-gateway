package com.llm.gateway.llm_gateway.application.service;

import com.llm.gateway.llm_gateway.domain.model.ChatRequest;
import com.llm.gateway.llm_gateway.domain.port.LLMProvider;
import com.llm.gateway.llm_gateway.dto.ExecutionMetadata;
import com.llm.gateway.llm_gateway.dto.GatewayRequest;
import com.llm.gateway.llm_gateway.infrastructure.config.ModelRegistry;
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
    private final ModelRegistry modelRegistry;

    // Spring injects all implementations of LLMProvider into a Map
    // Key = Bean Name (e.g., "openAIProvider", "mockProvider")
    public RouterService(Map<String, LLMProvider> providers, ModelRegistry modelRegistry) {
        this.providers = providers;
        this.modelRegistry= modelRegistry;
        LLMProvider openai = providers.get("openai");
        if (openai != null) {
            System.out.println(">>> INJECTED CLASS: " + openai.getClass().getName());
        }
    }

    public ExecutionMetadata routeAndExecute(GatewayRequest request, Consumer<String> chunkHandler) {
        String currentModelId = request.model();

        var config = getModelConfig(currentModelId);

        String targetModel = request.model();
        try {
            // Attempt 1: Primary Provider
//            log.info("Routing to primary provider for model: {}", targetModel);
//            primary.streamChat(request, chunkHandler);
            return execute(currentModelId, config.getProvider(), request, chunkHandler);
//            return new ExecutionMetadata("openai", targetModel);

        }
//        catch (CallNotPermittedException e) {
//            // Case A: Circuit is OPEN (OpenAI is known to be down)
//            log.warn("Circuit Open for {}. Failing over to Groq.", targetModel);
//            return executeFallback(request, chunkHandler);
//
//        }
        catch (Exception e) {
            log.warn("Primary model {} failed. Checking fallbacks...", currentModelId);

            if (config.getFallbacks() != null) {
                for (String fallbackModelId : config.getFallbacks()) {
                    try {
                        log.info(">>> Attempting Fallback: {}", fallbackModelId);

                        var fallbackConfig = getModelConfig(fallbackModelId);
                        GatewayRequest fallbackReq = new GatewayRequest(
                                fallbackModelId,
                                request.messages(),
                                request.temperature(),
                                request.stream(),
                                request.metadata()
                        );

                        return execute(fallbackModelId, fallbackConfig.getProvider(), fallbackReq, chunkHandler);

                    } catch (Exception fallbackError) {
                        log.warn("Fallback {} failed. Trying next...", fallbackModelId);
                        // Continue loop to next fallback
                    }
                }
            }
            // Case C: Unknown Error (500s, Network, etc.)
            log.error("Unexpected error from {}. Failing over.", targetModel, e);
            throw new RuntimeException("All providers exhausted. System is down.");        }
    }

    private LLMProvider getProvider(String model) {
        // Basic routing logic
        if (model.startsWith("llama")) return providers.get("groq");
        return providers.get("openai");
    }

    private ExecutionMetadata execute(String model, String providerName, GatewayRequest req, Consumer<String> handler) {
        LLMProvider provider = providers.get(providerName);
        if (provider == null) throw new IllegalArgumentException("Unknown provider: " + providerName);

        provider.streamChat(req, handler);
        return new ExecutionMetadata(providerName, model);
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

    private ModelRegistry.ModelConfig getModelConfig(String modelId) {
        var config = modelRegistry.getModels().get(modelId);
        if (config == null) throw new IllegalArgumentException("Model not configured in registry: " + modelId);
        return config;
    }
}
