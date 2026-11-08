package com.cleanlearning.adapters.database.user.entity;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The public fields of an expert profile. The shop's offering detail reads them through a projection in the offering
 * adapter (D-23).
 */
@Entity
@Table(name = "expert_profile")
public class ExpertProfileEntity {

    // The expert's user id.
    @Id
    private UUID userId;

    private String displayName;

    private String bio;

    protected ExpertProfileEntity() {
        // for JPA
    }
}
