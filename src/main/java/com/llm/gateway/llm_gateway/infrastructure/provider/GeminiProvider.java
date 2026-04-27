package com.llm.gateway.llm_gateway.infrastructure.provider;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Part;
import com.llm.gateway.llm_gateway.domain.model.GatewayRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service("google")
public class GeminiProvider implements LLMProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);

    private final Client client;

    public GeminiProvider(Client client) {
        this.client = client;
    }

    @Override
    @CircuitBreaker(name = "gemini")
    public void streamChat(GatewayRequest request, Consumer<String> chunkHandler) {

        var configBuilder = GenerateContentConfig.builder();
        List<Content> chatHistory = new ArrayList<>();

        // 1. Internal Mapping (DTO -> Gemini SDK)
        if (request.messages() != null) {
            for (GatewayRequest.MessageDto msg : request.messages()) {

                if ("system".equalsIgnoreCase(msg.role())) {
                    // System instructions go into the ConfigBuilder, not the message list
                    configBuilder.systemInstruction(
                            Content.builder()
                                    .parts(List.of(Part.builder().text(msg.content()).build()))
                                    .build()
                    );
                } else {
                    // Map "assistant" -> "model", "user" -> "user"
                    String geminiRole = "assistant".equalsIgnoreCase(msg.role()) ? "model" : "user";

                    // Add the entire conversation history, not just the last user query
                    chatHistory.add(
                            Content.builder()
                                    .role(geminiRole)
                                    .parts(List.of(Part.builder().text(msg.content()).build()))
                                    .build()
                    );
                }
            }
        }

        if (request.temperature() != null) {
            configBuilder.temperature(request.temperature().floatValue());
        }

        // 2. Execution
        try {
            // Pass the model, the chat history list, and the config object
            client.models.generateContentStream(request.model(), chatHistory, configBuilder.build())
                    .forEach(chunk -> {
                        // Gemini makes extracting the text much easier!
                        String text = chunk.text();
                        if (text != null && !text.isEmpty()) {
                            chunkHandler.accept(text);
                        }
                    });

        } catch (Exception e) {
            log.error("Error during Gemini streaming: ", e);
            throw e;
        }
    }
}