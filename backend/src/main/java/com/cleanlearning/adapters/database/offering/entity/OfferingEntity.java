package com.cleanlearning.adapters.database.offering.entity;

import java.util.UUID;

import com.cleanlearning.domain.offering.value.OfferingStatus;
import com.cleanlearning.domain.offering.value.OfferingType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Read through the projections in {@code OfferingRepository} and {@code ConsultationOfferingRepository}, and read and
 * written by {@code ConsultationOfferingAdapter}. The database sets {@code created_at} and {@code updated_at}.
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

    @Enumerated(EnumType.STRING)
    private OfferingStatus status;

    protected OfferingEntity() {
        // for JPA
    }

    public OfferingEntity(UUID id, OfferingType type, String name, String description, OfferingStatus status) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.description = description;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OfferingStatus getStatus() {
        return status;
    }

    public void setStatus(OfferingStatus status) {
        this.status = status;
    }
}
