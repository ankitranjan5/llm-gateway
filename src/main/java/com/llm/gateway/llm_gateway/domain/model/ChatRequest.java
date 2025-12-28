package com.llm.gateway.llm_gateway.domain.model;

import java.util.List;

// Using Java Records for immutability (Standard in Java 21)
public record ChatRequest(
        String model,
        List<Message> messages,
        boolean stream
) {}