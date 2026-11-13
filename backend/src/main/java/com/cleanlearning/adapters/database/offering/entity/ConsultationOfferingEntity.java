package com.cleanlearning.adapters.database.offering.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "consultation_offering")
public class ConsultationOfferingEntity {

  /**
   * Primary key and foreign key to {@link OfferingEntity}'s id: one row per consultation offering.
   */
  @Id private UUID offeringId;

  private int sessionDurationMinutes;

  private int cancellationDeadlineHours;

  protected ConsultationOfferingEntity() {
    // for JPA
  }

  public ConsultationOfferingEntity(
      UUID offeringId, int sessionDurationMinutes, int cancellationDeadlineHours) {
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
