package com.cleanlearning.api.consultation.response;

import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import com.cleanlearning.domain.consultation.view.CustomerUpcomingAppointment;
import java.time.Instant;
import java.util.UUID;

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
