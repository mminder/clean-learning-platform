package com.cleanlearning.adapters.database.offering;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Read through the projection in {@link OfferingRepository}.
 */
@Entity
@Table(name = "consultation_offering")
class ConsultationOfferingEntity {

    // Shares its id with offering.
    @Id
    private UUID offeringId;

    private int sessionDurationMinutes;

    private int cancellationDeadlineHours;

    protected ConsultationOfferingEntity() {
        // for JPA
    }
}
