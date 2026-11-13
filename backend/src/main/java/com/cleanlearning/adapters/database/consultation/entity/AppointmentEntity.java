package com.cleanlearning.adapters.database.consultation.entity;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Its expert is its case's assigned expert. The status is a {@link StoredAppointmentStatus}; a booking writes
 * {@code BOOKED}, and a cancellation changes it to {@code CANCELLED} with {@code creditRefunded}.
 */
@Entity
@Table(name = "appointment")
public class AppointmentEntity {

    @Id
    private UUID id;

    private UUID consultationCaseId;

    private Instant startsAt;

    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    private StoredAppointmentStatus status;

    // Whether the cancellation refunded the credit; null while BOOKED (UC-07).
    private @Nullable Boolean creditRefunded;

    protected AppointmentEntity() {
        // for JPA
    }

    public AppointmentEntity(UUID id, UUID consultationCaseId, Instant startsAt, Instant endsAt,
            StoredAppointmentStatus status) {
        this.id = id;
        this.consultationCaseId = consultationCaseId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConsultationCaseId() {
        return consultationCaseId;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public StoredAppointmentStatus getStatus() {
        return status;
    }

    public @Nullable Boolean getCreditRefunded() {
        return creditRefunded;
    }

    public void setStatus(StoredAppointmentStatus status) {
        this.status = status;
    }

    public void setCreditRefunded(@Nullable Boolean creditRefunded) {
        this.creditRefunded = creditRefunded;
    }
}
