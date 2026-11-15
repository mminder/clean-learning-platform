package com.cleanlearning.domain.consultation.view;

import com.cleanlearning.domain.consultation.Appointment;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.shared.LinkWindow;
import com.cleanlearning.domain.user.value.TeamsLink;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * The server computes the cancellation preview and the link window; the frontend never compares a
 * time with a deadline or a window. The Teams link is the assigned expert's, only while the window
 * is open (UC-10, D-19); one with a closed window is refused, as it never reaches the customer.
 */
public record CustomerUpcomingAppointment(
    CustomerAppointment appointment,
    boolean canCancel,
    Instant freeCancellationUntil,
    boolean creditRefundedIfCancelledNow,
    Instant linkAvailableFrom,
    boolean linkWindowOpen,
    Optional<TeamsLink> teamsLink) {

  public CustomerUpcomingAppointment {
    Objects.requireNonNull(appointment, "appointment");
    Objects.requireNonNull(freeCancellationUntil, "freeCancellationUntil");
    Objects.requireNonNull(linkAvailableFrom, "linkAvailableFrom");
    Objects.requireNonNull(teamsLink, "teamsLink");
    if (teamsLink.isPresent() && !linkWindowOpen) {
      throw new IllegalArgumentException(
          "A customer gets a call's link only inside its link window.");
    }
  }

  public static CustomerUpcomingAppointment of(
      Appointment appointment,
      CancellationDeadline deadline,
      Optional<TeamsLink> expertTeamsLink,
      Instant asOf) {
    boolean linkWindowOpen = LinkWindow.isOpenAt(appointment.start(), appointment.end(), asOf);
    return new CustomerUpcomingAppointment(
        CustomerAppointment.of(appointment, asOf),
        appointment.isCancellableAt(asOf),
        appointment.freeCancellationUntil(deadline),
        appointment.isCreditRefundedIfCancelledAt(deadline, asOf),
        LinkWindow.opensAt(appointment.start()),
        linkWindowOpen,
        linkWindowOpen ? expertTeamsLink : Optional.empty());
  }
}
