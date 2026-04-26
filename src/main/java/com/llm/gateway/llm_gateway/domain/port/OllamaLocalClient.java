package com.llm.gateway.llm_gateway.domain.port;

import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name="gateway.embedding.provider", havingValue = "ollama")
public class OllamaLocalClient implements EmbeddingClient {

    private final OllamaEmbeddingModel ollamaEmbeddingModel;

    public OllamaLocalClient(OllamaEmbeddingModel ollamaEmbeddingModel){
        this.ollamaEmbeddingModel = ollamaEmbeddingModel;
    }
    @Override
    public List<Float> embed(String text){
        float[] embeddings = ollamaEmbeddingModel.embed(text);

        List<Float> floats = new java.util.ArrayList<>(embeddings.length);
        for(float e: embeddings)floats.add(e);

        return floats;
    }

    @Override
    public int getDimensions(){
        return 768;
    }

}
