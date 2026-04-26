package com.llm.gateway.llm_gateway.domain.service;


import com.llm.gateway.llm_gateway.domain.model.User;
import com.llm.gateway.llm_gateway.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<String> getUserGroups(String userId) {
        if (userId == null || userId.isEmpty()) {
            return List.of("public"); // Guest access
        }

        return userRepository.findById(userId)
                .map(User::getGroups)
                .orElse(List.of("public")); // Fallback if user ID unknown
    }
}
