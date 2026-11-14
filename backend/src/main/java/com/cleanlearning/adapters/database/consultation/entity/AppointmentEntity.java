package com.cleanlearning.adapters.database.consultation.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "appointment")
public class AppointmentEntity {

  @Id private UUID id;

  private UUID consultationCaseId;

  private Instant startsAt;

  private Instant endsAt;

  @Enumerated(EnumType.STRING)
  private StoredAppointmentStatus status;

  // Null while BOOKED.
  private @Nullable Boolean creditRefunded;

  protected AppointmentEntity() {}

  public AppointmentEntity(
      UUID id,
      UUID consultationCaseId,
      Instant startsAt,
      Instant endsAt,
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
