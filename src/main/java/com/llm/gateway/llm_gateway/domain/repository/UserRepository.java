package com.llm.gateway.llm_gateway.domain.repository;

import com.llm.gateway.llm_gateway.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, String> {
}
