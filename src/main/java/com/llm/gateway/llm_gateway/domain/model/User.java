package com.llm.gateway.llm_gateway.domain.model;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "user_id") // Maps to 'user_id' sent in API metadata
    private String userId;    // e.g., "alice" or "u-12345"

    // Maps Java List<String> -> Postgres text[]
    @Type(ListArrayType.class)
    @Column(name = "user_groups", columnDefinition = "text[]")
    private List<String> groups;

    // Default constructor for JPA
    public User() {}

    public User(String userId, List<String> groups) {
        this.userId = userId;
        this.groups = groups;
    }

    public List<String> getGroups() { return groups; }
}
