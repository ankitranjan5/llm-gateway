package com.llm.gateway.llm_gateway.domain.port;

import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.EmbeddingCreateParams;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class EmbeddingConfig {

    @Bean
    public EmbeddingClient embeddingClient(@Qualifier("openaiClient") OpenAIClient client) {
        return new OpenAIEmbeddingClient(client);
    }

    // Implementation stays here (or can move to infrastructure/provider)
    public static class OpenAIEmbeddingClient implements EmbeddingClient {
        private final OpenAIClient client;

        public OpenAIEmbeddingClient(OpenAIClient client) {
            this.client = client;
        }

        @Override
        public List<Float> embed(String text) {
            // Updated to match typical OpenAI Java SDK syntax more closely
            var request = EmbeddingCreateParams.builder()
                    .model("text-embedding-3-small")
                    .input(text)
                    .build();

            var response = client.embeddings().create(request);

            // Extract vector from response
            return response.data().get(0).embedding();
        }
    }
}
