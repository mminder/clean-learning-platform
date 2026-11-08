package com.cleanlearning.adapters.database.offering.entity;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Read through the projection in {@code OfferingRepository}.
 */
@Entity
@Table(name = "consultation_offering")
public class ConsultationOfferingEntity {

    // Shares its id with offering.
    @Id
    private UUID offeringId;

    private int sessionDurationMinutes;

    private int cancellationDeadlineHours;

    protected ConsultationOfferingEntity() {
        // for JPA
    }
}
