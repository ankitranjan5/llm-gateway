package com.llm.gateway.llm_gateway.service;

import com.llm.gateway.llm_gateway.domain.model.GatewayRequest;

import java.util.List;
import java.util.Map;

public class SmartRouterService {

    private final RouterService routerService;

    public SmartRouterService(RouterService routerService) {
        this.routerService = routerService;
    }

    public boolean isComplexQuery(String userPrompt) {
        String systemInstruction = """
        You are a routing system. Analyze the user prompt.
        If it requires complex reasoning, coding, formatting, or heavy analysis, return exactly 'COMPLEX'.
        If it is a simple greeting, basic fact, or short summary, return exactly 'SIMPLE'.
        Do not output any other text.
    """;

        GatewayRequest classificationRequest = new GatewayRequest("llama-3.1-8b-instant",
                List.of(new GatewayRequest.MessageDto("system", systemInstruction),
                        new GatewayRequest.MessageDto("user", userPrompt)),
        0.0, false, Map.of());

        // Call your provider execution directly (blocking for the single word)
        StringBuilder result = new StringBuilder();
        routerService.determineQueryComplexity(classificationRequest, result::append);

        return result.toString().trim().toUpperCase().contains("COMPLEX");
    }
}
