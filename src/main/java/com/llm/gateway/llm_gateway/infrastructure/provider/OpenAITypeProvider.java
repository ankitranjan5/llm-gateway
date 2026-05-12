package com.llm.gateway.llm_gateway.infrastructure.provider;

import com.llm.gateway.llm_gateway.service.RouterService;
import com.llm.gateway.llm_gateway.domain.model.GatewayRequest;
import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class OpenAITypeProvider implements LLMProvider {

    private static final Logger log = LoggerFactory.getLogger(RouterService.class);

    private final OpenAIClient client;

    public OpenAITypeProvider(OpenAIClient client) {
        this.client = client;
    }

    @Override
    public void streamChat(GatewayRequest request, Consumer<String> chunkHandler) {
        ChatCompletionCreateParams params = mapToOpenAI(request);

        try {
            if (!request.shouldStream()) {
                ChatCompletion completion = client.chat().completions().create(params);
                completion.choices().stream()
                        .findFirst()
                        .flatMap(c -> c.message().content())
                        .ifPresent(chunkHandler::accept);
                return;
            }

            client.chat().completions().createStreaming(params).stream()
                    .forEach(chunk -> {
                        if (!chunk.choices().isEmpty()) {
                            chunk.choices().get(0).delta().content().ifPresent(chunkHandler::accept);
                        }
                    });
        } catch (Exception e) {
            log.error("Error during OpenAI completion: ", e);
            throw e;
        }
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
