package com.llm.gateway.llm_gateway.domain.service;

import com.llm.gateway.llm_gateway.infrastructure.config.ModelRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CostCalculator {

    private static final Logger log = LoggerFactory.getLogger(CostCalculator.class);
    private final ModelRegistry modelRegistry;

    // A lightweight data carrier for our results
    public record CostResult(BigDecimal actualCost, BigDecimal theoreticalCost) {}

    public CostCalculator(ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

    public CostResult calculateCost(String modelUsed, String modelRequested, int inputTokens, int outputTokens) {
        // 1. Calculate Actual Cost
        List<BigDecimal> actualRates = getCost(modelUsed);
        BigDecimal actualInCost = calculateTokenCost(actualRates.get(0), inputTokens);
        BigDecimal actualOutCost = calculateTokenCost(actualRates.get(1), outputTokens);
        BigDecimal actualCost = actualInCost.add(actualOutCost);

        // Default theoretical to actual (assume no downgrade happened)
        BigDecimal theoreticalCost = actualCost;

        // 2. Calculate Theoretical Cost (If downgraded)
        if (!modelUsed.equalsIgnoreCase(modelRequested)) {
            List<BigDecimal> theoreticalRates = getCost(modelRequested); // <-- THE BUG FIX
            BigDecimal theoreticalInCost = calculateTokenCost(theoreticalRates.get(0), inputTokens);
            BigDecimal theoreticalOutCost = calculateTokenCost(theoreticalRates.get(1), outputTokens);
            theoreticalCost = theoreticalInCost.add(theoreticalOutCost);
        }

        return new CostResult(actualCost, theoreticalCost);
    }

    // Helper method to keep the math clean and avoid repeating the division logic
    private BigDecimal calculateTokenCost(BigDecimal rate, int tokens) {
        return rate.multiply(BigDecimal.valueOf(tokens))
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
    }

    private List<BigDecimal> getCost(String modelName) {
        BigDecimal inputRate = BigDecimal.ZERO;
        BigDecimal outputRate = BigDecimal.ZERO;

        try {
            ModelRegistry.ModelConfig config = modelRegistry.getModels().get(modelName);

            if (config != null) {
                inputRate = config.getInputCost();
                outputRate = config.getOutputCost();
            } else {
                log.warn("Model '{}' not found in ModelRegistry. Defaulting cost to 0.", modelName);
            }
        } catch (Exception e) {
            log.error("Error calculating cost for model: {}", modelName, e);
        }

        return List.of(inputRate, outputRate);
    }
}