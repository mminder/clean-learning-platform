package com.cleanlearning.domain.consultation.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.cleanlearning.domain.consultation.Appointment;
import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.user.value.TeamsLink;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * UC-10 for the customer's upcoming appointment: the time its link window opens, whether it's open,
 * and the assigned expert's Teams link only while it is (D-19, spec 06, decision 19). Instants are
 * in UTC; Zurich is UTC+1 in November.
 */
class CustomerUpcomingAppointmentTest {

  /** Tue 17 Nov 2026, 10:00–11:00 in Zurich. */
  private static final Appointment TUESDAY =
      new Appointment(
          new AppointmentId(UUID.randomUUID()),
          new ConsultationCaseId(UUID.randomUUID()),
          Instant.parse("2026-11-17T09:00:00Z"),
          Instant.parse("2026-11-17T10:00:00Z"));

  /** 09:00 in Zurich. */
  private static final Instant OPENS = Instant.parse("2026-11-17T08:00:00Z");

  private static final Optional<TeamsLink> LINK =
      Optional.of(new TeamsLink("https://example.com/meet/expert-2"));

  private static CustomerUpcomingAppointment at(Instant asOf, Optional<TeamsLink> expertLink) {
    return CustomerUpcomingAppointment.of(TUESDAY, new CancellationDeadline(24), expertLink, asOf);
  }

  @Test
  void beforeTheWindowItHasTheTimeItOpensButNoLink() {
    CustomerUpcomingAppointment upcoming = at(OPENS.minusSeconds(60), LINK);

    assertThat(upcoming.linkAvailableFrom()).isEqualTo(OPENS);
    assertThat(upcoming.linkWindowOpen()).isFalse();
    assertThat(upcoming.teamsLink()).isEmpty();
  }

  @Test
  void insideTheWindowItHasTheExpertsLink() {
    CustomerUpcomingAppointment upcoming = at(OPENS, LINK);

    assertThat(upcoming.linkAvailableFrom()).isEqualTo(OPENS);
    assertThat(upcoming.linkWindowOpen()).isTrue();
    assertThat(upcoming.teamsLink()).isEqualTo(LINK);
  }

  /** Spec 06, decision 2: consumed from its start, and upcoming with its link until its end. */
  @Test
  void aRunningAppointmentHasTheLinkUntilItEnds() {
    CustomerUpcomingAppointment running = at(Instant.parse("2026-11-17T09:30:00Z"), LINK);

    assertThat(running.appointment().status()).isEqualTo(AppointmentStatus.CONSUMED);
    assertThat(running.linkWindowOpen()).isTrue();
    assertThat(running.teamsLink()).isEqualTo(LINK);
    assertThat(at(TUESDAY.end().minusNanos(1), LINK).teamsLink()).isEqualTo(LINK);
  }

  /** Spec 06, decision 10: the window is open, but the expert has no link yet. */
  @Test
  void withoutALinkAtTheExpertTheWindowOpensWithoutOne() {
    CustomerUpcomingAppointment before = at(OPENS.minusSeconds(60), Optional.empty());
    CustomerUpcomingAppointment inside = at(OPENS, Optional.empty());

    assertThat(before.linkWindowOpen()).isFalse();
    assertThat(before.teamsLink()).isEmpty();
    assertThat(inside.linkAvailableFrom()).isEqualTo(OPENS);
    assertThat(inside.linkWindowOpen()).isTrue();
    assertThat(inside.teamsLink()).isEmpty();
  }

  /** security.md's shaped responses: a link with a closed window is a programming error. */
  @Test
  void refusesALinkWithAClosedWindow() {
    CustomerUpcomingAppointment closed = at(OPENS.minusSeconds(60), LINK);

    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new CustomerUpcomingAppointment(
                closed.appointment(),
                closed.canCancel(),
                closed.freeCancellationUntil(),
                closed.creditRefundedIfCancelledNow(),
                OPENS,
                false,
                LINK))
        .withMessage("A customer gets a call's link only inside its link window.");
  }
}
