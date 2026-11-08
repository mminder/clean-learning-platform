package com.cleanlearning.adapters.database.user.entity;

import java.util.UUID;

import com.cleanlearning.domain.user.value.Role;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Read through the projection in {@code UserRepository}.
 */
@Entity
@Table(name = "user_role")
@IdClass(UserRoleEntity.Key.class)
public class UserRoleEntity {

    @Id
    private UUID userId;

    @Id
    @Enumerated(EnumType.STRING)
    private Role role;

    protected UserRoleEntity() {
        // for JPA
    }

    record Key(UUID userId, Role role) {
    }
}
