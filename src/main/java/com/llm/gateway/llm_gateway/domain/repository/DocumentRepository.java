package com.llm.gateway.llm_gateway.domain.repository;

import com.llm.gateway.llm_gateway.domain.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<DocumentChunk, UUID> {
}
