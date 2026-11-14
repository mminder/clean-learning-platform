package com.cleanlearning.domain.consultation.view;

import com.cleanlearning.domain.consultation.Appointment;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import java.time.Instant;
import java.util.Objects;

/**
 * The server computes the cancellation preview; the frontend never compares a time with a deadline.
 */
public record CustomerUpcomingAppointment(
    CustomerAppointment appointment,
    boolean canCancel,
    Instant freeCancellationUntil,
    boolean creditRefundedIfCancelledNow) {

  public CustomerUpcomingAppointment {
    Objects.requireNonNull(appointment, "appointment");
    Objects.requireNonNull(freeCancellationUntil, "freeCancellationUntil");
  }

  public static CustomerUpcomingAppointment of(
      Appointment appointment, CancellationDeadline deadline, Instant asOf) {
    return new CustomerUpcomingAppointment(
        CustomerAppointment.of(appointment, asOf),
        appointment.isCancellableAt(asOf),
        appointment.freeCancellationUntil(deadline),
        appointment.isCreditRefundedIfCancelledAt(deadline, asOf));
  }
}
