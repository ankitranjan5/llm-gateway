package com.llm.gateway.llm_gateway.domain.model;

import ch.qos.logback.core.read.ListAppender;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(columnDefinition = "TEXT")
    private String content;

    // The Vector Embedding (1536 dimensions for OpenAI small)
    @Column(columnDefinition = "vector(1536)")
//    @Type(com.llm.gateway.llm_gateway.domain.type.VectorType.class) // Hibernate handling needed
    private float[] embedding;

    // --- THE ACL LAYER ---
    // Who can see this? e.g., ["engineering", "admin", "user_123"]
    @Type(ListArrayType.class)
    @Column(name = "allowed_groups", columnDefinition = "text[]")
    private List<String> allowedGroups;

    // Metadata for citations
    private String sourceUrl;
}
