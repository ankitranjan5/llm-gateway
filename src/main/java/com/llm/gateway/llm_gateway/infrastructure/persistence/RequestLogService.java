package com.llm.gateway.llm_gateway.infrastructure.persistence;


import com.llm.gateway.llm_gateway.domain.service.CostCalculator;
import com.llm.gateway.llm_gateway.domain.model.GatewayRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


@Component
public class RequestLogService {

    private final RequestLogRepository repository; // Standard JPA Repository
    private final CostCalculator costCalculator;

    public RequestLogService(RequestLogRepository repository, CostCalculator costCalculator) {
        this.repository = repository;
        this.costCalculator = costCalculator;
    }

    @Async // <--- Runs in background thread
    public void log(GatewayRequest req, String provider, String usedModel,
                    int inTokens, int outTokens, long latency, boolean success) {

        BigDecimal cost = costCalculator.calculateCost(provider, usedModel, inTokens, outTokens);

        RequestLog log = RequestLog.builder()
                .requestId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .userId("test-user") // Hardcoded for now, extract from token later
                .provider(provider)
                .modelRequested(req.model())
                .modelUsed(usedModel)
                .inputTokens(inTokens)
                .outputTokens(outTokens)
                .totalTokens(inTokens + outTokens)
                .costUsd(cost)
                .latencyMs(latency)
                .status(success ? "SUCCESS" : "FAILURE")
                .build();

        repository.save(log);
    }
}
