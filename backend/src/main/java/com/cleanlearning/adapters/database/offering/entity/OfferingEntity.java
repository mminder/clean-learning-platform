package com.cleanlearning.adapters.database.offering.entity;

import java.util.UUID;

import com.cleanlearning.domain.offering.OfferingType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Read through the projection in {@code OfferingRepository}.
 */
@Entity
@Table(name = "offering")
public class OfferingEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private OfferingType type;

    private String name;

    private String description;

    private String status;

    protected OfferingEntity() {
        // for JPA
    }
}
