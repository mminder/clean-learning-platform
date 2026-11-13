package com.cleanlearning.adapters.database.consultation.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.cleanlearning.domain.consultation.value.CaseStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The purchase writes {@code ACTIVE}. Its first booking sets the assigned expert, and its cancellation the status,
 * {@code cancelled_at} and {@code refund_eligible}.
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

    @Enumerated(EnumType.STRING)
    private CaseStatus status;

    // Both null while the case is ACTIVE, both set once it's CANCELLED (V10's checks).
    private @Nullable Instant cancelledAt;

    private @Nullable Boolean refundEligible;

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
        this.status = CaseStatus.ACTIVE;
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

    public CaseStatus getStatus() {
        return status;
    }

    public void setStatus(CaseStatus status) {
        this.status = status;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public void setRefundEligible(boolean refundEligible) {
        this.refundEligible = refundEligible;
    }
}
