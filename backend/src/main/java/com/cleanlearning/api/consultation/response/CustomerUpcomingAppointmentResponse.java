package com.cleanlearning.api.consultation.response;

import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import com.cleanlearning.domain.consultation.view.CustomerUpcomingAppointment;
import com.cleanlearning.domain.user.value.TeamsLink;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * The server computes the cancellation preview and the link window: {@code linkAvailableFrom} is
 * one hour before the start, {@code linkWindowOpen} whether now is inside the window, and {@code
 * teamsLink} the assigned expert's link only while it is, {@code null} otherwise or without one
 * (UC-10, D-19).
 */
public record CustomerUpcomingAppointmentResponse(
    UUID id,
    Instant start,
    Instant end,
    AppointmentStatus status,
    boolean canCancel,
    Instant freeCancellationUntil,
    boolean creditRefundedIfCancelledNow,
    Instant linkAvailableFrom,
    boolean linkWindowOpen,
    @Nullable String teamsLink) {

  public static CustomerUpcomingAppointmentResponse from(CustomerUpcomingAppointment upcoming) {
    return new CustomerUpcomingAppointmentResponse(
        upcoming.appointment().id().value(),
        upcoming.appointment().start(),
        upcoming.appointment().end(),
        upcoming.appointment().status(),
        upcoming.canCancel(),
        upcoming.freeCancellationUntil(),
        upcoming.creditRefundedIfCancelledNow(),
        upcoming.linkAvailableFrom(),
        upcoming.linkWindowOpen(),
        upcoming.teamsLink().map(TeamsLink::value).orElse(null));
  }
}
