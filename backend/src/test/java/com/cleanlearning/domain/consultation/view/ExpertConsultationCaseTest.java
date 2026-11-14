package com.cleanlearning.domain.consultation.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.cleanlearning.domain.consultation.Appointment;
import com.cleanlearning.domain.consultation.CancelledAppointment;
import com.cleanlearning.domain.consultation.CaseCredits;
import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * A case as its assigned expert sees it at a given time (UC-11 step 4): the upcoming appointments,
 * a running one among them as consumed (spec 06, decision 2), the past ones, and the cancelled ones
 * in a list of their own, newest start first (decision 14). Instants are in UTC; Zurich is UTC+1 in
 * November.
 */
class ExpertConsultationCaseTest {

  private static final ConsultationCaseId CASE = new ConsultationCaseId(UUID.randomUUID());

  /** Mon 9 Nov 2026, 15:00 in Zurich. */
  private static final Instant NOW = Instant.parse("2026-11-09T14:00:00Z");

  /** Mon 16 Nov 2026, 10:00–11:00 in Zurich. */
  private static final Appointment MONDAY =
      appointment("2026-11-16T09:00:00Z", "2026-11-16T10:00:00Z");

  /** Tue 17 Nov 2026, 10:00–11:00 in Zurich. */
  private static final Appointment TUESDAY =
      appointment("2026-11-17T09:00:00Z", "2026-11-17T10:00:00Z");

  /** Wed 18 Nov 2026, 10:00–11:00 in Zurich. */
  private static final Appointment WEDNESDAY =
      appointment("2026-11-18T09:00:00Z", "2026-11-18T10:00:00Z");

  private static Appointment appointment(String start, String end) {
    return new Appointment(
        new AppointmentId(UUID.randomUUID()), CASE, Instant.parse(start), Instant.parse(end));
  }

  private static CancelledAppointment cancelled(String id, String start, boolean creditRefunded) {
    Instant startsAt = Instant.parse(start);
    return new CancelledAppointment(
        new AppointmentId(UUID.fromString(id)),
        startsAt,
        startsAt.plusSeconds(3_600),
        creditRefunded);
  }

  private static ExpertConsultationCase consultationCase(
      List<Appointment> booked, List<CancelledAppointment> cancelled, Instant asOf) {
    return new ExpertConsultationCase(
        CASE,
        "Customer 1",
        "customer1@example.com",
        new OfferingName("Career Coaching"),
        new SessionDuration(60),
        new CancellationDeadline(24),
        Instant.parse("2026-11-09T13:03:12.345Z"),
        new CaseCredits(new SessionCount(5), booked, cancelled),
        Optional.empty(),
        asOf);
  }

  private static List<AppointmentId> ids(List<ExpertAppointment> appointments) {
    return appointments.stream().map(ExpertAppointment::id).toList();
  }

  @Test
  void listsTheUpcomingSoonestFirstAndThePastNewestFirst() {
    ExpertConsultationCase consultationCase =
        consultationCase(
            List.of(WEDNESDAY, MONDAY, TUESDAY), List.of(), Instant.parse("2026-11-17T12:00:00Z"));

    assertThat(ids(consultationCase.upcomingAppointments())).containsExactly(WEDNESDAY.id());
    assertThat(consultationCase.upcomingAppointments())
        .extracting(ExpertAppointment::status)
        .containsExactly(AppointmentStatus.BOOKED);
    assertThat(ids(consultationCase.pastAppointments())).containsExactly(TUESDAY.id(), MONDAY.id());
    assertThat(consultationCase.pastAppointments())
        .extracting(ExpertAppointment::status)
        .containsOnly(AppointmentStatus.CONSUMED);
  }

  @Test
  void beforeAnyStartEveryBookedAppointmentIsUpcoming() {
    ExpertConsultationCase consultationCase =
        consultationCase(List.of(TUESDAY, MONDAY), List.of(), NOW);

    assertThat(ids(consultationCase.upcomingAppointments()))
        .containsExactly(MONDAY.id(), TUESDAY.id());
    assertThat(consultationCase.pastAppointments()).isEmpty();
  }

  /** Spec 06, decision 2: consumed from its start, upcoming until its end. */
  @Test
  void aRunningAppointmentIsUpcomingAndConsumedUntilItEnds() {
    ExpertConsultationCase atStart = consultationCase(List.of(TUESDAY), List.of(), TUESDAY.start());
    assertThat(atStart.upcomingAppointments())
        .singleElement()
        .satisfies(appointment -> {
          assertThat(appointment.id()).isEqualTo(TUESDAY.id());
          assertThat(appointment.status()).isEqualTo(AppointmentStatus.CONSUMED);
        });
    assertThat(atStart.pastAppointments()).isEmpty();

    ExpertConsultationCase justBefore =
        consultationCase(List.of(TUESDAY), List.of(), TUESDAY.start().minusSeconds(1));
    assertThat(justBefore.upcomingAppointments())
        .extracting(ExpertAppointment::status)
        .containsExactly(AppointmentStatus.BOOKED);

    ExpertConsultationCase atEnd = consultationCase(List.of(TUESDAY), List.of(), TUESDAY.end());
    assertThat(atEnd.upcomingAppointments()).isEmpty();
    assertThat(ids(atEnd.pastAppointments())).containsExactly(TUESDAY.id());
  }

  /**
   * Spec 06, decision 14: newest start first, refunded or forfeited alike, then by id as text, at
   * any time.
   */
  @Test
  void listsTheCancelledAppointmentsNewestStartFirstThenById() {
    CancelledAppointment monday =
        cancelled("00000000-0000-0000-0000-00000000000c", "2026-11-16T09:00:00Z", true);
    CancelledAppointment tuesdayB =
        cancelled("00000000-0000-0000-0000-00000000000b", "2026-11-17T09:00:00Z", false);
    CancelledAppointment tuesdayA =
        cancelled("00000000-0000-0000-0000-00000000000a", "2026-11-17T09:00:00Z", true);

    ExpertConsultationCase consultationCase =
        consultationCase(List.of(WEDNESDAY), List.of(monday, tuesdayB, tuesdayA), NOW);

    assertThat(consultationCase.cancelledAppointments())
        .containsExactly(tuesdayA, tuesdayB, monday);
    assertThat(ids(consultationCase.upcomingAppointments())).containsExactly(WEDNESDAY.id());
  }
}
