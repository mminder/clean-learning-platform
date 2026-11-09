package com.cleanlearning.adapters.database.availability.entity;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Written by {@code AvailabilityAdapter}, and read through the query in {@code AvailabilityRepository}. The database
 * sets {@code created_at} and {@code updated_at}.
 */
@Entity
@Table(name = "availability_date")
@IdClass(AvailabilityDateEntity.Key.class)
public class AvailabilityDateEntity {

    @Id
    private UUID expertId;

    @Id
    private LocalDate availableOn;

    protected AvailabilityDateEntity() {
        // for JPA
    }

    public AvailabilityDateEntity(UUID expertId, LocalDate availableOn) {
        this.expertId = expertId;
        this.availableOn = availableOn;
    }

    public record Key(UUID expertId, LocalDate availableOn) {
    }
}
