package com.llm.gateway.llm_gateway.domain.model;

import com.fasterxml.jackson.databind.JsonNode;

public record ExecutionMetadata(String provider, String modelRequested, String modelUsed, JsonNode toolCalls) {}
