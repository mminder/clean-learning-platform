package com.cleanlearning.domain.consultation.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.cleanlearning.domain.consultation.Appointment;
import com.cleanlearning.domain.consultation.CaseCredits;
import com.cleanlearning.domain.consultation.CaseInList;
import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseGroup;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.user.value.DisplayName;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * A case on "My purchases" at a given time (UC-05, spec 04 decision 21): its next appointment as
 * the customer sees it, and its group, which is {@link CaseInList}'s at that time; {@code
 * CaseInListTest} covers the groups and the order of the list. Instants are in UTC; Zurich is UTC+1
 * in November.
 */
class CustomerConsultationCaseSummaryTest {

  /** Mon 9 Nov 2026, 15:00 in Zurich. */
  private static final Instant NOW = Instant.parse("2026-11-09T14:00:00Z");

  /** Tue 17 Nov 2026, 10:00–11:00 in Zurich. */
  private static final Instant START = Instant.parse("2026-11-17T09:00:00Z");

  private static final Instant END = Instant.parse("2026-11-17T10:00:00Z");

  /** A case with 60-minute appointments booked at these starts, shown at {@code asOf}. */
  private static CustomerConsultationCaseSummary summary(
      int sessionCount, Instant asOf, String... startsOfAppointments) {
    ConsultationCaseId id = new ConsultationCaseId(UUID.randomUUID());
    List<Appointment> appointments =
        Arrays.stream(startsOfAppointments)
            .map(Instant::parse)
            .map(start ->
                new Appointment(
                    new AppointmentId(UUID.randomUUID()), id, start, start.plusSeconds(3_600)))
            .toList();
    return new CustomerConsultationCaseSummary(
        new CaseInList(
            id,
            Instant.parse("2026-11-09T13:03:12.345Z"),
            Optional.empty(),
            new CaseCredits(new SessionCount(sessionCount), appointments, List.of())),
        new OfferingName("Career Coaching"),
        Optional.empty(),
        Optional.of(new DisplayName("Expert 2")),
        asOf);
  }

  private static CustomerConsultationCaseSummary withAppointment(int sessionCount, Instant asOf) {
    return summary(sessionCount, asOf, START.toString());
  }

  @Test
  void aCaseWithoutAppointmentsHasNoNextAppointmentAndNeedsBooking() {
    CustomerConsultationCaseSummary summary = summary(3, NOW);

    assertThat(summary.group()).isEqualTo(PurchaseGroup.NEEDS_BOOKING);
    assertThat(summary.nextAppointment()).isEmpty();
  }

  @Test
  void aCaseWithAnUpcomingAppointmentShowsItAndIsUpcoming() {
    CustomerConsultationCaseSummary summary = withAppointment(3, NOW);

    assertThat(summary.group()).isEqualTo(PurchaseGroup.UPCOMING);
    assertThat(summary.nextAppointment())
        .hasValueSatisfying(next -> {
          assertThat(next.start()).isEqualTo(START);
          assertThat(next.end()).isEqualTo(END);
          assertThat(next.status()).isEqualTo(AppointmentStatus.BOOKED);
        });
  }

  /**
   * Spec 04, decision 21: consumed from its start, upcoming until its end, so its case is Upcoming
   * until then.
   */
  @Test
  void theNextAppointmentIsConsumedWhileItRunsAndGoneFromItsEnd() {
    assertThat(withAppointment(1, START.minusSeconds(1)).group()).isEqualTo(PurchaseGroup.UPCOMING);
    assertThat(withAppointment(1, START.minusSeconds(1)).nextAppointment().orElseThrow().status())
        .isEqualTo(AppointmentStatus.BOOKED);

    assertThat(withAppointment(1, START).group()).isEqualTo(PurchaseGroup.UPCOMING);
    assertThat(withAppointment(1, START).nextAppointment().orElseThrow().status())
        .isEqualTo(AppointmentStatus.CONSUMED);

    assertThat(withAppointment(1, END.minusSeconds(1)).group()).isEqualTo(PurchaseGroup.UPCOMING);
    assertThat(withAppointment(1, END.minusSeconds(1)).nextAppointment().orElseThrow().status())
        .isEqualTo(AppointmentStatus.CONSUMED);

    assertThat(withAppointment(1, END).group()).isEqualTo(PurchaseGroup.PAST);
    assertThat(withAppointment(1, END).nextAppointment()).isEmpty();
  }

  @Test
  void aCaseWithPastAppointmentsAndAnOpenCreditHasNoNextAppointment() {
    assertThat(withAppointment(3, END).group()).isEqualTo(PurchaseGroup.NEEDS_BOOKING);
    assertThat(withAppointment(3, END).nextAppointment()).isEmpty();
  }

  @Test
  void theNextAppointmentIsTheSoonestUpcomingOne() {
    CustomerConsultationCaseSummary summary =
        summary(3, NOW, "2026-11-19T09:00:00Z", "2026-11-17T09:00:00Z", "2026-11-18T09:00:00Z");

    assertThat(summary.nextAppointment().orElseThrow().start()).isEqualTo(START);
  }

  @Test
  void theNextAppointmentSkipsThePastOnes() {
    CustomerConsultationCaseSummary summary =
        summary(
            3,
            Instant.parse("2026-11-18T09:30:00Z"),
            "2026-11-17T09:00:00Z",
            "2026-11-19T09:00:00Z");

    assertThat(summary.nextAppointment().orElseThrow().start())
        .isEqualTo(Instant.parse("2026-11-19T09:00:00Z"));
  }
}
