package com.llm.gateway.llm_gateway.infrastructure.provider;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.ChatModel;
import com.llm.gateway.llm_gateway.dto.GatewayRequest;
import com.llm.gateway.llm_gateway.domain.port.LLMProvider;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service("openAIProvider")
public class OpenAIProvider implements LLMProvider {

    private final OpenAIClient client;

    public OpenAIProvider(OpenAIClient client) {
        this.client = client;
    }

    @Override
    public void streamChat(GatewayRequest request, Consumer<String> chunkHandler) {
        // 1. Internal Mapping (DTO -> OpenAI SDK)
        ChatCompletionCreateParams params = mapToOpenAI(request);

        // 2. Execution
        client.chat().completions().createStreaming(params).stream()
                .forEach(chunk -> {
                    if (chunk.choices().size() > 0) {
                        chunk.choices().get(0).delta().content().ifPresent(chunkHandler::accept);
                    }
                });
    }

    // Private helper: Keeps the messiness of mapping hidden inside this class
    private ChatCompletionCreateParams mapToOpenAI(GatewayRequest dto) {
        var builder = ChatCompletionCreateParams.builder()
                .model(ChatModel.of(dto.model()));

        if (dto.messages() != null) {
            dto.messages().forEach(msg -> {
                switch (msg.role().toLowerCase()) {
                    case "user" -> builder.addUserMessage(msg.content());
                    case "system" -> builder.addSystemMessage(msg.content());
                    case "assistant" -> builder.addAssistantMessage(msg.content());
                }
            });
        }

        if (dto.temperature() != null) builder.temperature(dto.temperature());

        return builder.build();
    }
}