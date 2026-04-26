package com.llm.gateway.llm_gateway.domain.service;

import com.llm.gateway.llm_gateway.domain.port.EmbeddingClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.pgvector.PGvector;

import java.sql.Connection;
import java.util.List;

@Service
public class RetrievalService {

    private final JdbcTemplate jdbcTemplate; // Native SQL is best for Vector Math

    private final EmbeddingClient embeddingClient;

    public RetrievalService (JdbcTemplate jdbcTemplate,
                            EmbeddingClient embeddingClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingClient = embeddingClient;
    }


    public List<String> retrieveContext(String query, List<String> userGroups) {
        // 1. Convert Query to Vector (using OpenAI Embedding API)
        List<Float> queryVector = embeddingClient.embed(query);

        // 2. The ACL-Aware SQL Query
        // logic: (Cosine Similarity) AND (User has permission)
        String sql = """
            SELECT content 
            FROM document_chunks 
            WHERE allowed_groups && ?::text[]  -- Postgres 'Overlaps' operator
            ORDER BY embedding <=> ?::vector   -- Cosine Distance
            LIMIT 3
        """;

        return jdbcTemplate.query(sql,
                ps -> {
                    // --- FIX FOR createSqlArray ---
                    // 1. Get the connection from the PreparedStatement
                    Connection conn = ps.getConnection();

                    // 2. Create the java.sql.Array manually
                    java.sql.Array sqlArray = conn.createArrayOf("text", userGroups.toArray());

                    // 3. Set the array
                    ps.setArray(1, sqlArray);

                    // --- FIX FOR Vector ---
                    // You must wrap the List in a PGvector object
                    ps.setObject(2, new PGvector(queryVector));
                },
                (rs, rowNum) -> rs.getString("content")
        );
    }
}
