package com.llm.gateway.llm_gateway.domain.model;

import java.util.List;

public record DocumentIngestionRequest(String content, List<String> allowedGroups) {}
