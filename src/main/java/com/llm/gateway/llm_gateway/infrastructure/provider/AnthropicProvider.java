package com.llm.gateway.llm_gateway.infrastructure.provider;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.MessageParam.Role;
import com.llm.gateway.llm_gateway.domain.model.GatewayRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service("anthropic")
public class AnthropicProvider implements LLMProvider {

    private static final Logger log = LoggerFactory.getLogger(AnthropicProvider.class);

    private final AnthropicClient client; // Inject your Anthropic SDK Client

    public AnthropicProvider(AnthropicClient client) {
        this.client = client;
    }

    @Override
    public void streamChat(GatewayRequest request, Consumer<String> chunkHandler) {

        var builder = MessageCreateParams.builder()
                .model(request.model())
                .maxTokens(4096); // Anthropic requires max_tokens to be set

        if (request.temperature() != null) {
            builder.temperature(request.temperature());
        }

        StringBuilder systemPrompt = new StringBuilder();

        // We use a simple intermediate list to handle the merging logic safely
        List<MergedMessage> mergedMessages = new ArrayList<>();

        // 1. The "Strict Alternating" Parser
        if (request.messages() != null) {
            for (GatewayRequest.MessageDto msg : request.messages()) {

                if ("system".equalsIgnoreCase(msg.role())) {
                    // Extract system prompt entirely
                    systemPrompt.append(msg.content()).append("\n");
                    continue;
                }

                String currentRole = "assistant".equalsIgnoreCase(msg.role()) ? "assistant" : "user";

                // Merge consecutive messages of the same role
                if (!mergedMessages.isEmpty() &&
                        mergedMessages.get(mergedMessages.size() - 1).role.equals(currentRole)) {

                    MergedMessage lastMessage = mergedMessages.get(mergedMessages.size() - 1);
                    lastMessage.content += "\n\n" + msg.content();
                } else {
                    // Add new message
                    mergedMessages.add(new MergedMessage(currentRole, msg.content()));
                }
            }
        }

        // 2. Apply to Anthropic Builder
        if (systemPrompt.length() > 0) {
            // Anthropic SDK handles system prompts at the top-level config
            builder.system(systemPrompt.toString().trim());
        }

        for (MergedMessage msg : mergedMessages) {
            if ("user".equals(msg.role)) {
                builder.addUserMessage(msg.content);
            } else {
                // If the SDK has an explicit addAssistantMessage, use it.
                // Otherwise, use the standard MessageParam builder:
                builder.addMessage(MessageParam.builder()
                        .role(Role.ASSISTANT)
                        .content(msg.content)
                        .build());
            }
        }

        // 3. Execution (Streaming)
        try {
            // Assuming your SDK exposes the standard .stream() method for Server-Sent Events
            client.messages().createStreaming(builder.build()).stream()
                    .forEach(event -> {
                        // The Anthropic stream returns different event types.
                        // We only care about the delta text.
                        event.contentBlockDelta().ifPresent(deltaEvent->{
                            deltaEvent.delta().text().ifPresent(ev->chunkHandler.accept(ev.text()));
                        });
                    });
        } catch (Exception e) {
            log.error("Error during Claude streaming: ", e);
            throw e;
        }
    }

    // Simple DTO for our merging logic to keep things clean
    private static class MergedMessage {
        String role;
        String content;

        MergedMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}