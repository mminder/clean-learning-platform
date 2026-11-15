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
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * A case on "My cases" at a given time (UC-11, spec 06 decision 2): its next appointment as its
 * expert sees it, and its group, which is {@link CaseInList}'s at that time; {@code CaseInListTest}
 * covers the groups and the order of the list. Instants are in UTC; Zurich is UTC+1 in November.
 */
class ExpertConsultationCaseSummaryTest {

  /** Mon 9 Nov 2026, 15:00 in Zurich. */
  private static final Instant NOW = Instant.parse("2026-11-09T14:00:00Z");

  /** Tue 17 Nov 2026, 10:00–11:00 in Zurich. */
  private static final Instant START = Instant.parse("2026-11-17T09:00:00Z");

  private static final Instant END = Instant.parse("2026-11-17T10:00:00Z");

  /** A case with 60-minute appointments booked at these starts, shown at {@code asOf}. */
  private static ExpertConsultationCaseSummary summary(
      int sessionCount, Instant asOf, Instant... startsOfAppointments) {
    ConsultationCaseId id = new ConsultationCaseId(UUID.randomUUID());
    List<Appointment> appointments =
        Arrays.stream(startsOfAppointments)
            .map(start ->
                new Appointment(
                    new AppointmentId(UUID.randomUUID()), id, start, start.plusSeconds(3_600)))
            .toList();
    return new ExpertConsultationCaseSummary(
        new CaseInList(
            id,
            Instant.parse("2026-11-09T13:03:12.345Z"),
            Optional.empty(),
            new CaseCredits(new SessionCount(sessionCount), appointments, List.of())),
        "Customer 1",
        new OfferingName("Career Coaching"),
        asOf);
  }

  @Test
  void aCaseWithAnUpcomingAppointmentShowsItAndIsUpcoming() {
    ExpertConsultationCaseSummary summary = summary(3, NOW, START);

    assertThat(summary.group()).isEqualTo(PurchaseGroup.UPCOMING);
    assertThat(summary.nextAppointment())
        .hasValueSatisfying(next -> {
          assertThat(next.start()).isEqualTo(START);
          assertThat(next.end()).isEqualTo(END);
          assertThat(next.status()).isEqualTo(AppointmentStatus.BOOKED);
        });
  }

  @Test
  void theNextAppointmentIsTheSoonestUpcomingOne() {
    ExpertConsultationCaseSummary summary =
        summary(
            3,
            Instant.parse("2026-11-18T09:30:00Z"),
            Instant.parse("2026-11-19T09:00:00Z"),
            START,
            Instant.parse("2026-11-20T09:00:00Z"));

    assertThat(summary.nextAppointment().orElseThrow().start())
        .isEqualTo(Instant.parse("2026-11-19T09:00:00Z"));
  }

  /**
   * Spec 06, decision 2: consumed from its start, upcoming until its end, so its case is Upcoming
   * until then.
   */
  @Test
  void aRunningAppointmentIsConsumedAndKeepsItsCaseUpcomingUntilItEnds() {
    ExpertConsultationCaseSummary running = summary(1, END.minusSeconds(1), START);
    assertThat(running.group()).isEqualTo(PurchaseGroup.UPCOMING);
    assertThat(running.nextAppointment().orElseThrow().status())
        .isEqualTo(AppointmentStatus.CONSUMED);

    ExpertConsultationCaseSummary ended = summary(1, END, START);
    assertThat(ended.group()).isEqualTo(PurchaseGroup.PAST);
    assertThat(ended.nextAppointment()).isEmpty();
  }

  @Test
  void aCancelledCaseIsPastAndHasNoNextAppointment() {
    ConsultationCaseId id = new ConsultationCaseId(UUID.randomUUID());
    ExpertConsultationCaseSummary summary =
        new ExpertConsultationCaseSummary(
            new CaseInList(
                id,
                Instant.parse("2026-11-09T13:03:12.345Z"),
                Optional.of(NOW),
                new CaseCredits(new SessionCount(3), List.of(), List.of())),
            "Customer 1",
            new OfferingName("Career Coaching"),
            NOW);

    assertThat(summary.group()).isEqualTo(PurchaseGroup.PAST);
    assertThat(summary.nextAppointment()).isEmpty();
  }

  @Test
  void aCaseWithAPastAppointmentAndOpenCreditsHasNoNextAppointmentAndNeedsBooking() {
    ExpertConsultationCaseSummary summary = summary(3, END, START);

    assertThat(summary.group()).isEqualTo(PurchaseGroup.NEEDS_BOOKING);
    assertThat(summary.nextAppointment()).isEmpty();
  }
}
