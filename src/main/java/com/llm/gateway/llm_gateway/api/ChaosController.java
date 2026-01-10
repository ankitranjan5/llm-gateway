package com.llm.gateway.llm_gateway.api;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chaos")
public class ChaosController {

    private final CircuitBreakerRegistry registry;

    public ChaosController(CircuitBreakerRegistry registry) {
        this.registry = registry;
    }

    // Force Open (Simulate Outage)
    @PostMapping("/break/{provider}")
    public String breakProvider(@PathVariable String provider) {
        registry.circuitBreaker(provider).transitionToOpenState();
        return "Circuit for " + provider + " is now OPEN. Requests will fail fast.";
    }

    // Restore (Simulate Recovery)
    @PostMapping("/fix/{provider}")
    public String fixProvider(@PathVariable String provider) {
        registry.circuitBreaker(provider).transitionToClosedState();
        return "Circuit for " + provider + " is now CLOSED. Requests will flow.";
    }

    @GetMapping("/list")
    public List<String> listBreakers() {
        return registry.getAllCircuitBreakers()
                .stream()
                .map(cb -> cb.getName() + " -> " + cb.getState())
                .toList();
    }
}
