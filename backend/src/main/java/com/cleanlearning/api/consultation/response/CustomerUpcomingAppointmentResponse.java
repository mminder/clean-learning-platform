package com.cleanlearning.api.consultation.response;

import com.cleanlearning.domain.consultation.read.CustomerUpcomingAppointment;
import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * An upcoming appointment on the case detail (UC-05, UC-07): {@link CustomerAppointmentResponse}'s
 * fields, and its cancellation preview at the time of the answer, which the cancel dialog reads
 * (spec 05, decision 12), all computed by the server. {@code canCancel}: it hasn't started. {@code
 * freeCancellationUntil}: start − the case's cancellation deadline, an instant, for the late
 * warning. {@code creditRefundedIfCancelledNow}: now is before it.
 */
public record CustomerUpcomingAppointmentResponse(
    UUID id,
    Instant start,
    Instant end,
    AppointmentStatus status,
    boolean canCancel,
    Instant freeCancellationUntil,
    boolean creditRefundedIfCancelledNow) {

  public static CustomerUpcomingAppointmentResponse from(CustomerUpcomingAppointment upcoming) {
    return new CustomerUpcomingAppointmentResponse(
        upcoming.appointment().id().value(),
        upcoming.appointment().start(),
        upcoming.appointment().end(),
        upcoming.appointment().status(),
        upcoming.canCancel(),
        upcoming.freeCancellationUntil(),
        upcoming.creditRefundedIfCancelledNow());
  }
}
