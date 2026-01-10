package com.llm.gateway.llm_gateway.infrastructure.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "gateway")
@Data
@Getter
public class ModelRegistry {

    private Map<String, ModelConfig> models;

    public Map<String, ModelConfig> getModels() { return models; }
    public void setModels(Map<String, ModelConfig> models) { this.models = models; }
    public ModelConfig modelConfig;

    // Inner class to match YAML structure
    public static class ModelConfig {
        private String provider;
        private BigDecimal inputCost;
        private BigDecimal outputCost;
        private List<String> fallbacks;

        // Getters and Setters
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public List<String> getFallbacks() { return fallbacks; }
        public void setFallbacks(List<String> fallbacks) { this.fallbacks = fallbacks; }
        public BigDecimal getInputCost() { return inputCost; }
        public void setInputCost(BigDecimal inputCost) { this.inputCost = inputCost; }
        public BigDecimal getOutputCost() { return outputCost; }
        public void setOutputCost(BigDecimal outputCost) { this.outputCost = outputCost; }
    }
}