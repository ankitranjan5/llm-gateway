package com.llm.gateway.llm_gateway.service;

import com.llm.gateway.llm_gateway.infrastructure.provider.LLMProvider;
import com.llm.gateway.llm_gateway.domain.service.RetrievalService;
import com.llm.gateway.llm_gateway.domain.service.UserService;
import com.llm.gateway.llm_gateway.domain.model.ExecutionMetadata;
import com.llm.gateway.llm_gateway.domain.model.GatewayRequest;
import com.llm.gateway.llm_gateway.infrastructure.config.ModelRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class RouterService {

    private static final Logger log = LoggerFactory.getLogger(RouterService.class);
    private final Map<String, LLMProvider> providers;
    private final ModelRegistry modelRegistry;
    private RetrievalService retrievalService;
    private UserService userService;

    // Spring injects all implementations of LLMProvider into a Map
    // Key = Bean Name (e.g., "openAIProvider", "mockProvider")
    public RouterService(Map<String, LLMProvider> providers, ModelRegistry modelRegistry, RetrievalService retrievalService,
                         UserService userService) {
        this.providers = providers;
        this.modelRegistry= modelRegistry;
        LLMProvider openai = providers.get("openai");
        if (openai != null) {
            System.out.println(">>> INJECTED CLASS: " + openai.getClass().getName());
        }
        this.retrievalService = retrievalService;
        this.userService = userService;
    }

    public void determineQueryComplexity(GatewayRequest request, Consumer<String> chunkHandler){
        String currentModelId = request.model();
        var config = getModelConfig(currentModelId);
        String providerName = config.getProvider();
        ExecutionMetadata metadata = execute(currentModelId,currentModelId, providerName, request, chunk -> {
            // No-op for this simple method
        });
    }

    public ExecutionMetadata routeAndExecute(GatewayRequest request, Consumer<String> chunkHandler) {
        String userQuery = request.messages().getLast().content();
        SmartRouterService smartRouter = new SmartRouterService(this);
        String currentModelId = request.model();
        String targetModel = request.model();

        if (!smartRouter.isComplexQuery(userQuery)) {
            log.info("🚀 Simple query detected. Downgrading to Llama-3-70b to save costs.");
            targetModel = "llama-3.1-8b-instant";
        }

        List<String> contextDocs = null;

        if(request.metadata() != null && request.metadata().get("user_id") != null) {
            String userId = request.metadata().get("user_id").toString();
            List<String> userGroups = userService.getUserGroups(userId);

            contextDocs = retrievalService.retrieveContext(
                    request.messages().getLast().content(),
                    userGroups
            );
        }

        GatewayRequest enrichedRequest = injectContextAndOptimizeModel(targetModel, request, contextDocs);

        var config = getModelConfig(targetModel);


        try {
            // Attempt 1: Primary Provider
//            log.info("Routing to primary provider for model: {}", targetModel);
//            primary.streamChat(request, chunkHandler);
            return execute(currentModelId, targetModel, config.getProvider(), enrichedRequest, chunkHandler);
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

                        return execute(fallbackModelId, fallbackModelId, fallbackConfig.getProvider(), fallbackReq, chunkHandler);

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

    private ExecutionMetadata execute(String modelRequested, String modelUsed, String providerName, GatewayRequest req, Consumer<String> handler) {
        LLMProvider provider = providers.get(providerName);
        if (provider == null) throw new IllegalArgumentException("Unknown provider: " + providerName);

        provider.streamChat(req, handler);
        return new ExecutionMetadata(providerName, modelRequested, modelUsed);
    }


    private ModelRegistry.ModelConfig getModelConfig(String modelId) {
        var config = modelRegistry.getModels().get(modelId);
        if (config == null) throw new IllegalArgumentException("Model not configured in registry: " + modelId);
        return config;
    }


    // Inside SmartRouterService.java

    private GatewayRequest injectContextAndOptimizeModel(String targetModel,GatewayRequest originalRequest, List<String> contextDocs) {
        if (contextDocs == null || contextDocs.isEmpty()) {
            return new GatewayRequest(
                    targetModel,
                    originalRequest.messages(),
                    originalRequest.temperature(),
                    originalRequest.stream(),
                    originalRequest.metadata()
            );
        }

        // 1. Format the Context Block
        String contextBlock = String.join("\n---\n", contextDocs);
        String systemInstruction = """
        \n[CONTEXT_START]
        The following documents are retrieved from the knowledge base.
        Use them to answer the user's question. If the answer is not in the context, say so.
        
        %s
        [CONTEXT_END]\n
        """.formatted(contextBlock);

        // 2. Prepare new Message List
        List<GatewayRequest.MessageDto> newMessages = new java.util.ArrayList<>(originalRequest.messages());

        // 3. Inject into System Prompt
        // Check if the first message is already a System Prompt
        if (!newMessages.isEmpty() && "system".equalsIgnoreCase(newMessages.get(0).role())) {
            var oldSystem = newMessages.get(0);

            // Create updated system message (Old Instructions + New Context)
            var updatedSystem = new GatewayRequest.MessageDto(
                    "system",
                    oldSystem.content() + systemInstruction
            );

            newMessages.set(0, updatedSystem);
        } else {
            // No System Prompt found? Add one at the top (Index 0)
            newMessages.add(0, new GatewayRequest.MessageDto("system", systemInstruction));
        }

        // 4. Return NEW Request (GatewayRequest is a Record, so it's immutable)
        return new GatewayRequest(
                targetModel,
                newMessages,
                originalRequest.temperature(),
                originalRequest.stream(),
                originalRequest.metadata()
        );
    }
}
