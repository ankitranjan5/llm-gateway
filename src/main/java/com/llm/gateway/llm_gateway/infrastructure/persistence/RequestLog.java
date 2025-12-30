package com.llm.gateway.llm_gateway.infrastructure.persistence;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "request_logs")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequestLog {
    @Id
    private String requestId;
    private LocalDateTime timestamp;

    private String userId;
    private String provider;
    private String modelRequested;
    private String modelUsed;

    private int inputTokens;
    private int outputTokens;
    private int totalTokens;

    @Column(precision = 10, scale = 6)
    private BigDecimal costUsd;

    private long latencyMs;
    private String status;
    private String errorMessage;
}
