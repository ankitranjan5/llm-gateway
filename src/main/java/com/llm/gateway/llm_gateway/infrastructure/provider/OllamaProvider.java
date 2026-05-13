package com.llm.gateway.llm_gateway.infrastructure.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.llm.gateway.llm_gateway.domain.model.GatewayRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
        streamChat(request, chunkHandler, null);
    }

    @Override
    public void streamChat(GatewayRequest request, Consumer<String> chunkHandler, Consumer<JsonNode> toolCallsConsumer) {
        log.info("Routing request to local Ollama (Model: {})", request.model());

        Map<String, Object> ollamaPayload = buildOllamaChatPayload(request);

        if (request.shouldStream()) {
            handleStreaming(ollamaPayload, chunkHandler);
            if (toolCallsConsumer != null) {
                toolCallsConsumer.accept(null);
            }
        } else {
            handleNonStreaming(ollamaPayload, chunkHandler, toolCallsConsumer);
        }
    }

    /**
     * Ollama {@code /api/chat} accepts OpenAI-style {@code tools} and optional {@code tool_choice}.
     */
    private Map<String, Object> buildOllamaChatPayload(GatewayRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", request.model());
        payload.put("messages", request.messages());
        payload.put("stream", request.shouldStream());
        List<Map<String, Object>> tools = request.tools();
        if (tools != null && !tools.isEmpty()) {
            payload.put("tools", coerceToolsForOllama(tools));
        }
        if (request.tool_choice() != null && !request.tool_choice().isBlank()) {
            Object choice = coerceToolChoice(request.tool_choice());
            if (choice != null) {
                payload.put("tool_choice", choice);
            }
        }
        return payload;
    }

    /**
     * Ensures each tool is {@code { "type": "function", "function": { name, description?, parameters } }}
     * and that {@code parameters} is a JSON object (Ollama expects a schema object, not a string).
     */
    private List<Map<String, Object>> coerceToolsForOllama(List<Map<String, Object>> tools) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> raw : tools) {
            if (raw == null) {
                continue;
            }
            Map<String, Object> function;
            Object fnObj = raw.get("function");
            if (fnObj instanceof Map<?, ?> fnMapRaw) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fn = new LinkedHashMap<>((Map<String, Object>) fnMapRaw);
                fn.put("parameters", coerceParametersObject(fn.get("parameters")));
                function = fn;
            } else if (raw.containsKey("name")) {
                function = new LinkedHashMap<>();
                function.put("name", raw.get("name"));
                if (raw.containsKey("description")) {
                    function.put("description", raw.get("description"));
                }
                function.put("parameters", coerceParametersObject(raw.get("parameters")));
            } else {
                log.warn("Skipping tool entry without function or name: {}", raw);
                continue;
            }

            Map<String, Object> tool = new LinkedHashMap<>();
            tool.put("type", "function");
            tool.put("function", function);
            result.add(tool);
        }
        return result;
    }

    private Map<String, Object> coerceParametersObject(Object params) {
        if (params == null) {
            return Map.of("type", "object", "properties", Map.of());
        }
        if (params instanceof String s) {
            try {
                JsonNode n = objectMapper.readTree(s);
                if (n.isObject()) {
                    return objectMapper.convertValue(n, new TypeReference<Map<String, Object>>() {});
                }
            } catch (Exception e) {
                log.warn("Could not parse tool parameters JSON string: {}", e.getMessage());
            }
            return Map.of("type", "object", "properties", Map.of());
        }
        if (params instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) m;
            return map;
        }
        return Map.of("type", "object", "properties", Map.of());
    }

    private Object coerceToolChoice(String toolChoice) {
        String trimmed = toolChoice.trim();
        if (trimmed.startsWith("{")) {
            try {
                return objectMapper.readValue(trimmed, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("Invalid tool_choice JSON, omitting: {}", e.getMessage());
                return null;
            }
        }
        return trimmed;
    }

    /**
     * Ollama sometimes returns {@code function.arguments} as a JSON string; normalize to a structured node
     * so clients receive consistent {@code JsonNode} arguments.
     */
    private JsonNode normalizeOllamaToolCalls(JsonNode toolCalls) {
        if (toolCalls == null || !toolCalls.isArray()) {
            return toolCalls;
        }
        ArrayNode out = objectMapper.createArrayNode();
        for (JsonNode call : toolCalls) {
            if (!call.isObject()) {
                out.add(call);
                continue;
            }
            ObjectNode c = call.deepCopy();
            JsonNode fn = c.get("function");
            if (fn instanceof ObjectNode fnObj && fnObj.has("arguments")) {
                JsonNode args = fnObj.get("arguments");
                if (args != null && args.isTextual()) {
                    try {
                        fnObj.set("arguments", objectMapper.readTree(args.asText()));
                    } catch (Exception ignored) {
                        // keep textual arguments as-is
                    }
                }
            }
            out.add(c);
        }
        return out;
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

    private void handleNonStreaming(
            Map<String, Object> payload,
            Consumer<String> chunkHandler,
            Consumer<JsonNode> toolCallsConsumer) {
        String responseJson = restClient.post()
                .uri("/api/chat")
                .body(payload)
                .retrieve()
                .body(String.class);

        try {
            JsonNode node = objectMapper.readTree(responseJson);
            JsonNode messageNode = node.path("message");
            String fullContent = messageNode.path("content").asText("");
            chunkHandler.accept(fullContent);

            if (toolCallsConsumer != null) {
                JsonNode toolCalls = messageNode.path("tool_calls");
                if (!toolCalls.isMissingNode() && !toolCalls.isNull()) {
                    toolCallsConsumer.accept(normalizeOllamaToolCalls(toolCalls));
                } else {
                    toolCallsConsumer.accept(null);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse non-streaming Ollama response", e);
            chunkHandler.accept("Error parsing LLM response.");
            if (toolCallsConsumer != null) {
                toolCallsConsumer.accept(null);
            }
        }
    }
}