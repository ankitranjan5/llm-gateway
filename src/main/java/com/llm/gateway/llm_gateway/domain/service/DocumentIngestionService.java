package com.llm.gateway.llm_gateway.domain.service;

import com.llm.gateway.llm_gateway.domain.model.DocumentChunk;
import com.llm.gateway.llm_gateway.domain.port.EmbeddingClient;
import com.llm.gateway.llm_gateway.domain.repository.DocumentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DocumentIngestionService {


    DocumentRepository documentRepository;
    EmbeddingClient embeddingClient;

    public DocumentIngestionService(DocumentRepository documentRepository, EmbeddingClient embeddingClient){
        this.documentRepository = documentRepository;
        this.embeddingClient = embeddingClient;
    }

    public void ingestDocument(String content, List<String> allowedGroups){
        DocumentChunk chunk = new DocumentChunk();
        chunk.setContent(content);
        chunk.setAllowedGroups(allowedGroups);

        List<Float> embedding = embeddingClient.embed(content);
        chunk.setEmbedding(convertListToArray(embedding));
        documentRepository.save(chunk);
    }

    private float[] convertListToArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

}
