package com.cleanlearning.domain.consultation;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import java.time.Instant;
import java.util.Objects;

/**
 * {@code BOOKED} and {@code CONSUMED} are derived at {@code asOf}, never stored (D-11). A running
 * appointment is consumed and upcoming at once, and can't be cancelled.
 */
public record Appointment(
    AppointmentId id, ConsultationCaseId consultationCaseId, Instant start, Instant end) {

  public Appointment {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(consultationCaseId, "consultationCaseId");
    Objects.requireNonNull(start, "start");
    Objects.requireNonNull(end, "end");
    if (!start.isBefore(end)) {
      throw new IllegalArgumentException(
          "An appointment ends after it starts: " + start + " to " + end + ".");
    }
  }

  public AppointmentStatus statusAt(Instant asOf) {
    return start.isAfter(asOf) ? AppointmentStatus.BOOKED : AppointmentStatus.CONSUMED;
  }

  public boolean isUpcomingAt(Instant asOf) {
    return end.isAfter(asOf);
  }

  /**
   * Real hours, not wall-clock: 24 hours before a start the day after a summer-time change is
   * another local time than the start's.
   */
  public Instant freeCancellationUntil(CancellationDeadline deadline) {
    return deadline.freeUntil(start);
  }

  public boolean isCancellableAt(Instant asOf) {
    return statusAt(asOf) == AppointmentStatus.BOOKED;
  }

  /** At the deadline itself the cancellation is late and the credit is forfeited. */
  public boolean isCreditRefundedIfCancelledAt(CancellationDeadline deadline, Instant asOf) {
    return asOf.isBefore(freeCancellationUntil(deadline));
  }
}
