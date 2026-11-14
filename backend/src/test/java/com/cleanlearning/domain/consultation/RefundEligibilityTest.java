package com.cleanlearning.domain.consultation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.NonRefundableReason;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.shared.BusinessTimeZone;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * UC-08's refund rule, as spec 05's "Cancel a case" scenarios describe it: 60-minute sessions, a
 * 24-hour deadline, and "now" is Mon 16 Nov 2026, 11:00 in Zurich, an hour after the free
 * cancellation of the appointment on 17 Nov 2026, 10:00–11:00 ended. Instants are written with
 * their Zurich time, so the tests read like the scenarios.
 */
class RefundEligibilityTest {

  private static final ConsultationCaseId CASE = new ConsultationCaseId(UUID.randomUUID());
  private static final CancellationDeadline DAY = new CancellationDeadline(24);

  private static final Instant NOW = zurich("2026-11-16T11:00");

  private static Instant zurich(String localDateTime) {
    return LocalDateTime.parse(localDateTime).atZone(BusinessTimeZone.ZONE).toInstant();
  }

  /** A 60-minute appointment of the case, starting at {@code start}. */
  private static Appointment appointment(Instant start) {
    return new Appointment(
        new AppointmentId(UUID.randomUUID()), CASE, start, start.plusSeconds(3600));
  }

  /** A 60-minute appointment of the case, starting at {@code start} in Zurich. */
  private static Appointment appointment(String start) {
    return appointment(zurich(start));
  }

  private static RefundEligibility rule(List<Appointment> booked, int forfeited) {
    return RefundEligibility.at(credits(booked, forfeited), DAY, NOW);
  }

  /** The credits of a case with these booked appointments and this many cancelled late. */
  private static CaseCredits credits(List<Appointment> booked, int forfeited) {
    return new CaseCredits(
        new SessionCount(booked.size() + forfeited + 1),
        booked,
        Stream.generate(() ->
                new CancelledAppointment(
                    new AppointmentId(UUID.randomUUID()), NOW, NOW.plusSeconds(3600), false))
            .limit(forfeited)
            .toList());
  }

  @Test
  void aCaseWithoutAppointmentsAndForfeitedCreditsIsRefundEligible() {
    RefundEligibility result = rule(List.of(), 0);

    assertThat(result.isRefundEligible()).isTrue();
    assertThat(result.reason()).isEmpty();
    assertThat(result.deadlinePassedAppointment()).isEmpty();
    assertThat(result.freeCancellationUntil()).isEmpty();
  }

  @Test
  void appointmentsBeforeTheirDeadlineAreRefundEligible() {
    assertThat(
            rule(List.of(appointment("2026-11-20T10:00"), appointment("2026-11-25T10:00")), 0)
                .isRefundEligible())
        .isTrue();
  }

  @Test
  void aConsumedAppointmentIsASessionThatTookPlace() {
    RefundEligibility result =
        rule(List.of(appointment("2026-11-10T10:00"), appointment("2026-11-25T10:00")), 0);

    assertThat(result.isRefundEligible()).isFalse();
    assertThat(result.reason()).contains(NonRefundableReason.SESSION_TOOK_PLACE);
    assertThat(result.deadlinePassedAppointment()).isEmpty();
    assertThat(result.freeCancellationUntil()).isEmpty();
  }

  @Test
  void aRunningAppointmentIsASessionThatTookPlace() {
    // 10:30–11:30: it started half an hour ago.
    assertThat(rule(List.of(appointment("2026-11-16T10:30")), 0).reason())
        .contains(NonRefundableReason.SESSION_TOOK_PLACE);
  }

  @Test
  void anAppointmentStartingNowIsASessionThatTookPlace() {
    assertThat(rule(List.of(appointment(NOW)), 0).reason())
        .contains(NonRefundableReason.SESSION_TOOK_PLACE);
  }

  @Test
  void aForfeitedCreditIsReportedAlone() {
    RefundEligibility result = rule(List.of(), 1);

    assertThat(result.isRefundEligible()).isFalse();
    assertThat(result.reason()).contains(NonRefundableReason.CREDIT_FORFEITED);
    assertThat(result.deadlinePassedAppointment()).isEmpty();
    assertThat(result.freeCancellationUntil()).isEmpty();
  }

  @Test
  void severalForfeitedCreditsAreTheSameReason() {
    assertThat(rule(List.of(), 3).reason()).contains(NonRefundableReason.CREDIT_FORFEITED);
  }

  @Test
  void anAppointmentPastItsDeadlineButNotStartedNamesItAndItsFreeCancellationTime() {
    // The spec's scenario: 17 Nov 2026, 10:00–11:00, free until 16 Nov 2026, 10:00.
    Appointment late = appointment("2026-11-17T10:00");
    RefundEligibility result = rule(List.of(late), 0);

    assertThat(result.isRefundEligible()).isFalse();
    assertThat(result.reason()).contains(NonRefundableReason.DEADLINE_PASSED);
    assertThat(result.deadlinePassedAppointment()).contains(late);
    assertThat(result.freeCancellationUntil()).contains(zurich("2026-11-16T10:00"));
  }

  @Test
  void exactlyAtTheDeadlineTheAppointmentCountsAsPassed() {
    Appointment appointment = appointment("2026-11-17T11:00");

    RefundEligibility result = rule(List.of(appointment), 0);

    assertThat(result.reason()).contains(NonRefundableReason.DEADLINE_PASSED);
    assertThat(result.deadlinePassedAppointment()).contains(appointment);
    assertThat(result.freeCancellationUntil()).contains(NOW);
  }

  @Test
  void aSecondBeforeTheDeadlineTheAppointmentIsStillFree() {
    Appointment appointment = appointment(zurich("2026-11-17T11:00").plusSeconds(1));

    assertThat(rule(List.of(appointment), 0).isRefundEligible()).isTrue();
  }

  @Test
  void withoutADeadlineAnAppointmentIsFreeUntilItStarts() {
    CancellationDeadline none = new CancellationDeadline(0);

    assertThat(
            RefundEligibility.at(credits(List.of(appointment(NOW.plusSeconds(1))), 0), none, NOW)
                .isRefundEligible())
        .isTrue();
    assertThat(RefundEligibility.at(credits(List.of(appointment(NOW)), 0), none, NOW).reason())
        .contains(NonRefundableReason.SESSION_TOOK_PLACE);
  }

  @Test
  void deadlinePassedNamesTheSoonestLateAppointmentWhateverTheListOrder() {
    // Today 22:00 and tomorrow 10:00 are both late; 30 Nov is free.
    Appointment soonestLate = appointment("2026-11-16T22:00");
    Appointment laterLate = appointment("2026-11-17T10:00");
    Appointment free = appointment("2026-11-30T10:00");

    for (List<Appointment> booked :
        List.of(List.of(free, laterLate, soonestLate), List.of(soonestLate, laterLate, free))) {
      RefundEligibility result = rule(booked, 0);

      assertThat(result.deadlinePassedAppointment()).contains(soonestLate);
      assertThat(result.freeCancellationUntil()).contains(zurich("2026-11-15T22:00"));
    }
  }

  @Test
  void aFreeAppointmentDoesNotHideALateOne() {
    // With a 7-day deadline, the appointment in 3 days is late and the one in 10 days isn't.
    CancellationDeadline week = new CancellationDeadline(168);
    Appointment late = appointment("2026-11-19T10:00");
    Appointment free = appointment("2026-11-26T10:00");

    RefundEligibility result = RefundEligibility.at(credits(List.of(free, late), 0), week, NOW);

    assertThat(result.deadlinePassedAppointment()).contains(late);
    assertThat(result.freeCancellationUntil()).contains(zurich("2026-11-12T10:00"));
  }

  @Test
  void aSessionThatTookPlaceComesBeforeAForfeitedCreditAndAPassedDeadline() {
    RefundEligibility result =
        rule(List.of(appointment("2026-11-10T10:00"), appointment("2026-11-17T10:00")), 1);

    assertThat(result.reason()).contains(NonRefundableReason.SESSION_TOOK_PLACE);
    assertThat(result.deadlinePassedAppointment()).isEmpty();
  }

  @Test
  void aSessionThatTookPlaceComesBeforeAForfeitedCredit() {
    // The spec's "Only the first reason".
    assertThat(rule(List.of(appointment("2026-11-10T10:00")), 2).reason())
        .contains(NonRefundableReason.SESSION_TOOK_PLACE);
  }

  @Test
  void aForfeitedCreditComesBeforeAPassedDeadline() {
    RefundEligibility result = rule(List.of(appointment("2026-11-17T10:00")), 1);

    assertThat(result.reason()).contains(NonRefundableReason.CREDIT_FORFEITED);
    assertThat(result.deadlinePassedAppointment()).isEmpty();
    assertThat(result.freeCancellationUntil()).isEmpty();
  }

  @Test
  void aRefundedCreditDoesNotCount() {
    // An appointment cancelled in time is neither booked nor forfeited, so nothing is left to
    // count.
    assertThat(rule(List.of(), 0).isRefundEligible()).isTrue();
    assertThat(rule(List.of(appointment("2026-11-25T10:00")), 0).isRefundEligible()).isTrue();
  }

  @Test
  void countsRealHoursAcrossTheChangeToSummerTime() {
    // Sun 28 Mar 2027, 10:00 (CEST): 24 real hours earlier is Sat 27 Mar, 09:00 (CET), not 10:00.
    List<Appointment> booked = List.of(appointment("2027-03-28T10:00"));

    assertThat(
            RefundEligibility.at(
                    credits(booked, 0), DAY, zurich("2027-03-27T09:00").minusSeconds(1))
                .isRefundEligible())
        .isTrue();
    RefundEligibility late =
        RefundEligibility.at(credits(booked, 0), DAY, zurich("2027-03-27T09:30"));
    assertThat(late.reason()).contains(NonRefundableReason.DEADLINE_PASSED);
    assertThat(late.freeCancellationUntil()).contains(zurich("2027-03-27T09:00"));
  }

  @Test
  void theConstructorRefusesAnAppointmentWithoutDeadlinePassed() {
    Appointment appointment = appointment("2026-11-17T10:00");
    Optional<Instant> until = Optional.of(zurich("2026-11-16T10:00"));

    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new RefundEligibility(
                Optional.of(NonRefundableReason.CREDIT_FORFEITED),
                Optional.of(appointment),
                until));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RefundEligibility(Optional.empty(), Optional.of(appointment), until));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RefundEligibility(Optional.empty(), Optional.empty(), until));
  }

  @Test
  void theConstructorRefusesDeadlinePassedWithoutTheAppointmentOrItsTime() {
    Appointment appointment = appointment("2026-11-17T10:00");
    Optional<NonRefundableReason> reason = Optional.of(NonRefundableReason.DEADLINE_PASSED);
    Optional<Instant> until = Optional.of(zurich("2026-11-16T10:00"));

    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RefundEligibility(reason, Optional.empty(), Optional.empty()));
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> new RefundEligibility(reason, Optional.of(appointment), Optional.empty()));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RefundEligibility(reason, Optional.empty(), until));
  }
}
