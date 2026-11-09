package com.cleanlearning.adapters.database.consultation.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Written by {@code ConsultationCaseAdapter}, and read through the projections in {@code ConsultationCaseRepository};
 * a booking reads and locks it as an entity, and its first booking sets the assigned expert. The database sets
 * {@code created_at} and {@code updated_at}.
 */
@Entity
@Table(name = "consultation_case")
public class ConsultationCaseEntity {

    @Id
    private UUID id;

    private UUID customerId;

    // consultation_offering shares its id with offering, so this is also the offering's id.
    private UUID consultationOfferingId;

    private BigDecimal pricePaid;

    private int sessionCount;

    private int sessionDurationMinutes;

    private int cancellationDeadlineHours;

    private Instant purchasedAt;

    // The expert's user id, which is also the id of their expert profile; null until the first booking (UC-06).
    private @Nullable UUID assignedExpertId;

    protected ConsultationCaseEntity() {
        // for JPA
    }

    public ConsultationCaseEntity(UUID id, UUID customerId, UUID consultationOfferingId, BigDecimal pricePaid,
            int sessionCount, int sessionDurationMinutes, int cancellationDeadlineHours, Instant purchasedAt) {
        this.id = id;
        this.customerId = customerId;
        this.consultationOfferingId = consultationOfferingId;
        this.pricePaid = pricePaid;
        this.sessionCount = sessionCount;
        this.sessionDurationMinutes = sessionDurationMinutes;
        this.cancellationDeadlineHours = cancellationDeadlineHours;
        this.purchasedAt = purchasedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConsultationOfferingId() {
        return consultationOfferingId;
    }

    public BigDecimal getPricePaid() {
        return pricePaid;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public int getSessionDurationMinutes() {
        return sessionDurationMinutes;
    }

    public int getCancellationDeadlineHours() {
        return cancellationDeadlineHours;
    }

    public @Nullable UUID getAssignedExpertId() {
        return assignedExpertId;
    }

    public void setAssignedExpertId(UUID assignedExpertId) {
        this.assignedExpertId = assignedExpertId;
    }
}
