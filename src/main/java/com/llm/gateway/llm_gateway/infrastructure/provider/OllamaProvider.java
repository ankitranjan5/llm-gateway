package com.llm.gateway.llm_gateway.infrastructure.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llm.gateway.llm_gateway.domain.model.GatewayRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service("ollama")
public class OllamaProvider implements LLMProvider {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OllamaProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // Pointing to default local Ollama port.
        // If Gateway is in Docker, use "http://host.docker.internal:11434"
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:11434")
                .build();
    }

    @Override
    public void streamChat(GatewayRequest request, Consumer<String> chunkHandler) {
        log.info("Routing request to local Ollama (Model: {})", request.model());

        // 1. Map your GatewayRequest to Ollama's expected JSON schema
        Map<String, Object> ollamaPayload = Map.of(
                "model", request.model(), // e.g., "llama3.1"
                "messages", request.messages(),
                "stream", request.shouldStream()
        );

        if (request.shouldStream()) {
            handleStreaming(ollamaPayload, chunkHandler);
        } else {
            handleNonStreaming(ollamaPayload, chunkHandler);
        }
    }

    private void handleStreaming(Map<String, Object> payload, Consumer<String> chunkHandler) {
        restClient.post()
                .uri("/api/chat")
                .body(payload)
                .exchange((clientRequest, clientResponse) -> {
                    // Read the NDJSON stream line-by-line as it arrives
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(clientResponse.getBody(), StandardCharsets.UTF_8))) {

                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.isBlank()) continue;

                            JsonNode node = objectMapper.readTree(line);
                            JsonNode messageNode = node.path("message");

                            if (!messageNode.isMissingNode() && messageNode.has("content")) {
                                chunkHandler.accept(messageNode.get("content").asText());
                            }
                        }
                    }
                    return null; // Required by exchange() signature
                });
    }

    private void handleNonStreaming(Map<String, Object> payload, Consumer<String> chunkHandler) {
        String responseJson = restClient.post()
                .uri("/api/chat")
                .body(payload)
                .retrieve()
                .body(String.class);

        try {
            JsonNode node = objectMapper.readTree(responseJson);
            String fullContent = node.path("message").path("content").asText("");
            chunkHandler.accept(fullContent);
        } catch (Exception e) {
            log.error("Failed to parse non-streaming Ollama response", e);
            chunkHandler.accept("Error parsing LLM response.");
        }
    }
}