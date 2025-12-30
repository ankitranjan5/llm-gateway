package com.llm.gateway.llm_gateway.domain.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CostCalculator {

    // Pricing (Per 1M Tokens)
    private static final BigDecimal GPT4_IN = new BigDecimal("5.00");
    private static final BigDecimal GPT4_OUT = new BigDecimal("15.00");

    private static final BigDecimal GROQ_LLAMA_IN = new BigDecimal("0.05"); // Approx
    private static final BigDecimal GROQ_LLAMA_OUT = new BigDecimal("0.08");

    public BigDecimal calculateCost(String provider, String model, int input, int output) {
        BigDecimal inputRate = BigDecimal.ZERO;
        BigDecimal outputRate = BigDecimal.ZERO;

        // Simple lookup logic
        if ("openai".equals(provider) && model.startsWith("gpt-4")) {
            inputRate = GPT4_IN;
            outputRate = GPT4_OUT;
        } else if ("groq".equals(provider)) {
            inputRate = GROQ_LLAMA_IN;
            outputRate = GROQ_LLAMA_OUT;
        }

        // Formula: (Tokens / 1,000,000) * Rate
        BigDecimal inCost = inputRate.multiply(BigDecimal.valueOf(input))
                .divide(BigDecimal.valueOf(1_000_000));
        BigDecimal outCost = outputRate.multiply(BigDecimal.valueOf(output))
                .divide(BigDecimal.valueOf(1_000_000));

        return inCost.add(outCost);
    }
}