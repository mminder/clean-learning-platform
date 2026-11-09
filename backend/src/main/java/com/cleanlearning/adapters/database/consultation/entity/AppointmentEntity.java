package com.cleanlearning.adapters.database.consultation.entity;

import java.time.Instant;
import java.util.UUID;

import com.cleanlearning.domain.consultation.value.AppointmentStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Written and read by {@code ConsultationCaseAdapter}. The database sets {@code created_at} and {@code updated_at}. Its
 * expert is its case's assigned expert. The status is only ever {@code BOOKED} (the column's check): {@code CONSUMED}
 * is derived, never stored (D-11).
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
    private AppointmentStatus status;

    protected AppointmentEntity() {
        // for JPA
    }

    public AppointmentEntity(UUID id, UUID consultationCaseId, Instant startsAt, Instant endsAt,
            AppointmentStatus status) {
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
}
