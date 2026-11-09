package com.cleanlearning.adapters.database.user.entity;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Read through the projection in {@code UserRepository}.
 */
@Entity
@Table(name = "app_user")
public class UserEntity {

    @Id
    private UUID id;

    private String name;

    private String email;

    protected UserEntity() {
        // for JPA
    }
}
