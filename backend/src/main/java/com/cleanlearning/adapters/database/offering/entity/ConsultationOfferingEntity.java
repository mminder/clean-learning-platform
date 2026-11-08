package com.cleanlearning.adapters.database.offering.entity;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Read through the projections in {@code OfferingRepository}, and read and written by
 * {@code ConsultationOfferingAdapter}. The database sets {@code created_at} and {@code updated_at}.
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

    public ConsultationOfferingEntity(UUID offeringId, int sessionDurationMinutes, int cancellationDeadlineHours) {
        this.offeringId = offeringId;
        this.sessionDurationMinutes = sessionDurationMinutes;
        this.cancellationDeadlineHours = cancellationDeadlineHours;
    }

    public int getSessionDurationMinutes() {
        return sessionDurationMinutes;
    }

    public void setSessionDurationMinutes(int sessionDurationMinutes) {
        this.sessionDurationMinutes = sessionDurationMinutes;
    }

    public int getCancellationDeadlineHours() {
        return cancellationDeadlineHours;
    }

    public void setCancellationDeadlineHours(int cancellationDeadlineHours) {
        this.cancellationDeadlineHours = cancellationDeadlineHours;
    }
}
