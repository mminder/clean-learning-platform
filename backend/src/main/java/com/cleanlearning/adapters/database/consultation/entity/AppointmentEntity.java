package com.cleanlearning.adapters.database.consultation.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Written and read by {@code ConsultationCaseAdapter}. The database sets {@code created_at} and {@code updated_at}. Its
 * expert is its case's assigned expert. The status is a {@link StoredAppointmentStatus}, only {@code BOOKED} for now.
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
}
