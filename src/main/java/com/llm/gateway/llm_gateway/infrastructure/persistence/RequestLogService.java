package com.llm.gateway.llm_gateway.infrastructure.persistence;


import com.llm.gateway.llm_gateway.domain.service.CostCalculator;
import com.llm.gateway.llm_gateway.domain.model.GatewayRequest;
import com.llm.gateway.llm_gateway.infrastructure.config.ModelRegistry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.swing.plaf.nimbus.NimbusStyle;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
    public void log(GatewayRequest req, String provider, String usedModel, String modelRequested,
                    int inTokens, int outTokens, long latency, boolean success) {

        CostCalculator.CostResult cost = costCalculator.calculateCost(usedModel,modelRequested, inTokens, outTokens);
        BigDecimal actualCost = cost.actualCost();
        BigDecimal theoreticalCost = cost.theoreticalCost();

        if(theoreticalCost.equals(BigDecimal.ZERO)){
            theoreticalCost = actualCost; // Avoid showing "savings" when we don't have cost data
        }

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
                .costUsd(actualCost)
                .costSavedUsd(theoreticalCost.subtract(actualCost))
                .latencyMs(latency)
                .status(success ? "SUCCESS" : "FAILURE")
                .build();

        repository.save(log);
    }
}
