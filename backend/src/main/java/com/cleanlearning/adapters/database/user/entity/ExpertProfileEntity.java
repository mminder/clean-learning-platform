package com.cleanlearning.adapters.database.user.entity;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The public fields of an expert profile.
 */
@Entity
@Table(name = "expert_profile")
public class ExpertProfileEntity {

    /** Primary key and foreign key to {@link UserEntity}'s id: one row per expert. */
    @Id
    private UUID userId;

    private String displayName;

    private String bio;

    protected ExpertProfileEntity() {
        // for JPA
    }
}
