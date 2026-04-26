package com.llm.gateway.llm_gateway.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestLogRepository extends JpaRepository<RequestLog, String> {
}
