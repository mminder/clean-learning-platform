package com.cleanlearning.domain.consultation.read;

import static org.assertj.core.api.Assertions.assertThat;

import com.cleanlearning.domain.consultation.Appointment;
import com.cleanlearning.domain.consultation.AssignedExpert;
import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import com.cleanlearning.domain.consultation.value.CancellationOutcome;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.NonRefundableReason;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.value.Money;
import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * A case as its customer sees it at a given time (UC-05's case detail): the credits, which every
 * booked appointment uses, consumed or not (spec 04, decision 15), and every forfeited one (spec
 * 05), whether the customer can book, and the upcoming appointments with their cancellation preview
 * (UC-07) and the past ones (spec 04, decision 21). Instants are in UTC; Zurich is UTC+1 in
 * November.
 */
class CustomerConsultationCaseTest {

  private static final ConsultationCaseId CASE = new ConsultationCaseId(UUID.randomUUID());

  private static final AssignedExpert EXPERT_2 =
      new AssignedExpert(
          new UserId(UUID.fromString("00000000-0000-0000-0000-000000000202")),
          new DisplayName("Expert 2"),
          "Leadership coach.");

  /** Mon 16 Nov 2026, 10:00–11:00 in Zurich. */
  private static final Appointment MONDAY =
      appointment("2026-11-16T09:00:00Z", "2026-11-16T10:00:00Z");

  /** Tue 17 Nov 2026, 10:00–11:00 in Zurich. */
  private static final Appointment TUESDAY =
      appointment("2026-11-17T09:00:00Z", "2026-11-17T10:00:00Z");

  /** Wed 18 Nov 2026, 10:00–11:00 in Zurich. */
  private static final Appointment WEDNESDAY =
      appointment("2026-11-18T09:00:00Z", "2026-11-18T10:00:00Z");

  /** Mon 9 Nov 2026, 15:00 in Zurich. */
  private static final Instant NOW = Instant.parse("2026-11-09T14:00:00Z");

  private static Appointment appointment(String start, String end) {
    return new Appointment(
        new AppointmentId(UUID.randomUUID()), CASE, Instant.parse(start), Instant.parse(end));
  }

  private static CustomerConsultationCase withSessions(int sessionCount) {
    return withSessions(sessionCount, Optional.empty(), List.of(), 0, NOW);
  }

  private static CustomerConsultationCase withSessions(
      int sessionCount,
      Optional<AssignedExpert> expert,
      List<Appointment> appointments,
      int forfeitedCredits,
      Instant asOf) {
    return withSessions(
        sessionCount, expert, appointments, forfeitedCredits, Optional.empty(), asOf);
  }

  private static CustomerConsultationCase withSessions(
      int sessionCount,
      Optional<AssignedExpert> expert,
      List<Appointment> appointments,
      int forfeitedCredits,
      Optional<CancellationOutcome> cancellation,
      Instant asOf) {
    return new CustomerConsultationCase(
        CASE,
        new OfferingName("Career Coaching"),
        Optional.empty(),
        new PurchaseTerms(
            Money.chf("450.00"),
            new SessionCount(sessionCount),
            new SessionDuration(60),
            new CancellationDeadline(24)),
        Instant.parse("2026-11-09T14:03:12.345Z"),
        expert,
        appointments,
        forfeitedCredits,
        cancellation,
        asOf);
  }

  /** Cancelled on Sun 15 Nov 2026, 12:00 in Zurich, with these appointments left booked. */
  private static CustomerConsultationCase cancelled(List<Appointment> appointments, Instant asOf) {
    return withSessions(
        3,
        Optional.of(EXPERT_2),
        appointments,
        0,
        Optional.of(new CancellationOutcome(Instant.parse("2026-11-15T11:00:00Z"), false)),
        asOf);
  }

  private static CustomerConsultationCase withAppointments(
      List<Appointment> appointments, Instant asOf) {
    return withSessions(3, Optional.of(EXPERT_2), appointments, 0, asOf);
  }

  @Test
  void totalCreditsAreTheSessionCount() {
    assertThat(withSessions(3).totalCredits()).isEqualTo(3);
    assertThat(withSessions(1).totalCredits()).isEqualTo(1);
  }

  @Test
  void withoutAppointmentsEveryCreditIsOpenAndTheCustomerCanBook() {
    assertThat(withSessions(3).openCredits()).isEqualTo(3);
    assertThat(withSessions(5).openCredits()).isEqualTo(5);
    assertThat(withSessions(3).canBook()).isTrue();
    assertThat(withSessions(3).expert()).isEmpty();
  }

  @Test
  void everyBookedAppointmentUsesACreditConsumedOrNot() {
    // At Tuesday 10:30: Monday's is past, Tuesday's runs, Wednesday's is ahead.
    CustomerConsultationCase consultationCase =
        withAppointments(List.of(MONDAY, TUESDAY), Instant.parse("2026-11-17T09:30:00Z"));

    assertThat(consultationCase.openCredits()).isEqualTo(1);
    assertThat(consultationCase.canBook()).isTrue();
  }

  @Test
  void withoutAnOpenCreditTheCustomerCantBook() {
    CustomerConsultationCase consultationCase =
        withAppointments(List.of(MONDAY, TUESDAY, WEDNESDAY), NOW);

    assertThat(consultationCase.openCredits()).isZero();
    assertThat(consultationCase.canBook()).isFalse();
  }

  @Test
  void beforeItsStartAnAppointmentIsUpcomingAndBooked() {
    CustomerConsultationCase consultationCase = withAppointments(List.of(WEDNESDAY, TUESDAY), NOW);

    assertThat(consultationCase.upcomingAppointments())
        .extracting(CustomerUpcomingAppointment::appointment)
        .containsExactly(
            new CustomerAppointment(
                TUESDAY.id(), TUESDAY.start(), TUESDAY.end(), AppointmentStatus.BOOKED),
            new CustomerAppointment(
                WEDNESDAY.id(), WEDNESDAY.start(), WEDNESDAY.end(), AppointmentStatus.BOOKED));
    assertThat(consultationCase.pastAppointments()).isEmpty();
  }

  @Test
  void fromItsStartAnAppointmentIsConsumedAndStillUpcoming() {
    CustomerConsultationCase atTheStart = withAppointments(List.of(TUESDAY), TUESDAY.start());
    CustomerConsultationCase whileItRuns =
        withAppointments(List.of(TUESDAY), Instant.parse("2026-11-17T09:30:00Z"));

    for (CustomerConsultationCase consultationCase : List.of(atTheStart, whileItRuns)) {
      assertThat(consultationCase.upcomingAppointments())
          .extracting(CustomerUpcomingAppointment::appointment)
          .containsExactly(
              new CustomerAppointment(
                  TUESDAY.id(), TUESDAY.start(), TUESDAY.end(), AppointmentStatus.CONSUMED));
      assertThat(consultationCase.pastAppointments()).isEmpty();
      assertThat(consultationCase.openCredits()).isEqualTo(2);
    }
  }

  @Test
  void fromItsEndAnAppointmentIsPastAndConsumed() {
    CustomerConsultationCase consultationCase = withAppointments(List.of(TUESDAY), TUESDAY.end());

    assertThat(consultationCase.upcomingAppointments()).isEmpty();
    assertThat(consultationCase.pastAppointments())
        .containsExactly(
            new CustomerAppointment(
                TUESDAY.id(), TUESDAY.start(), TUESDAY.end(), AppointmentStatus.CONSUMED));
    assertThat(consultationCase.openCredits()).isEqualTo(2);
  }

  @Test
  void upcomingSoonestFirstAndPastNewestFirst() {
    Appointment friday = appointment("2026-11-20T09:00:00Z", "2026-11-20T10:00:00Z");
    CustomerConsultationCase consultationCase =
        withSessions(
            5,
            Optional.of(EXPERT_2),
            List.of(TUESDAY, friday, MONDAY, WEDNESDAY),
            0,
            Instant.parse("2026-11-18T12:00:00Z"));

    assertThat(consultationCase.upcomingAppointments())
        .extracting(upcoming -> upcoming.appointment().id())
        .containsExactly(friday.id());
    assertThat(consultationCase.pastAppointments())
        .extracting(CustomerAppointment::id)
        .containsExactly(WEDNESDAY.id(), TUESDAY.id(), MONDAY.id());
  }

  /**
   * Spec 05: a forfeited credit is used and its appointment isn't shown; a refunded one is open
   * again.
   */
  @Test
  void aForfeitedCreditIsNotOpen() {
    // Monday's is consumed, one was cancelled in time and isn't here, one late
    CustomerConsultationCase consultationCase =
        withSessions(
            3, Optional.of(EXPERT_2), List.of(MONDAY), 1, Instant.parse("2026-11-17T12:00:00Z"));

    assertThat(consultationCase.openCredits()).isOne();
    assertThat(consultationCase.canBook()).isTrue();
    assertThat(consultationCase.pastAppointments())
        .extracting(CustomerAppointment::id)
        .containsExactly(MONDAY.id());
  }

  @Test
  void withEveryCreditForfeitedTheCustomerCantBook() {
    CustomerConsultationCase consultationCase =
        withSessions(1, Optional.of(EXPERT_2), List.of(), 1, NOW);

    assertThat(consultationCase.openCredits()).isZero();
    assertThat(consultationCase.canBook()).isFalse();
    assertThat(consultationCase.upcomingAppointments()).isEmpty();
    assertThat(consultationCase.pastAppointments()).isEmpty();
  }

  /**
   * UC-07's preview, which the cancel dialog reads: free until Mon 16 Nov 2026, 10:00 in Zurich,
   * 09:00Z.
   */
  @Test
  void anUpcomingAppointmentCarriesItsCancellationPreview() {
    Instant freeUntil = Instant.parse("2026-11-16T09:00:00Z");

    assertThat(
            withAppointments(List.of(TUESDAY), Instant.parse("2026-11-15T11:00:00Z"))
                .upcomingAppointments())
        .containsExactly(
            new CustomerUpcomingAppointment(
                CustomerAppointment.of(TUESDAY, Instant.parse("2026-11-15T11:00:00Z")),
                true,
                freeUntil,
                true));
    // at the deadline: late
    assertThat(withAppointments(List.of(TUESDAY), freeUntil).upcomingAppointments())
        .containsExactly(
            new CustomerUpcomingAppointment(
                CustomerAppointment.of(TUESDAY, freeUntil), true, freeUntil, false));
    // running: consumed, and it can't be cancelled
    assertThat(withAppointments(List.of(TUESDAY), TUESDAY.start()).upcomingAppointments())
        .containsExactly(
            new CustomerUpcomingAppointment(
                CustomerAppointment.of(TUESDAY, TUESDAY.start()), false, freeUntil, false));
  }

  @Test
  void thePreviewFollowsTheCasesDeadline() {
    CustomerConsultationCase seventyTwoHours =
        new CustomerConsultationCase(
            CASE,
            new OfferingName("Career Coaching"),
            Optional.empty(),
            new PurchaseTerms(
                Money.chf("450.00"),
                new SessionCount(3),
                new SessionDuration(60),
                new CancellationDeadline(72)),
            Instant.parse("2026-11-09T14:03:12.345Z"),
            Optional.of(EXPERT_2),
            List.of(TUESDAY),
            0,
            Optional.empty(),
            Instant.parse("2026-11-15T11:00:00Z"));

    assertThat(seventyTwoHours.upcomingAppointments())
        .singleElement()
        .satisfies(upcoming -> {
          assertThat(upcoming.canCancel()).isTrue();
          assertThat(upcoming.freeCancellationUntil())
              .isEqualTo(Instant.parse("2026-11-14T09:00:00Z"));
          assertThat(upcoming.creditRefundedIfCancelledNow()).isFalse();
        });
  }

  @Test
  void showsTheAssignedExpert() {
    assertThat(withAppointments(List.of(TUESDAY), NOW).expert()).contains(EXPERT_2);
  }

  /**
   * UC-08: an active case can be cancelled until it's completed, with its refund outcome as the
   * preview.
   */
  @Test
  void anActiveCaseCanBeCancelledWithItsRefundOutcome() {
    CustomerConsultationCase withoutAppointments = withSessions(3);

    assertThat(withoutAppointments.cancellation()).isEmpty();
    assertThat(withoutAppointments.canCancel()).isTrue();
    assertThat(withoutAppointments.refundIfCancelledNow())
        .hasValueSatisfying(refund -> assertThat(refund.isRefundEligible()).isTrue());
    // Mon 16 Nov 2026, 11:00 in Zurich: Tuesday's free cancellation ended at 10:00
    assertThat(
            withAppointments(List.of(TUESDAY), Instant.parse("2026-11-16T10:00:00Z"))
                .refundIfCancelledNow())
        .hasValueSatisfying(refund -> {
          assertThat(refund.reason()).contains(NonRefundableReason.DEADLINE_PASSED);
          assertThat(refund.deadlinePassedAppointment()).contains(TUESDAY);
          assertThat(refund.freeCancellationUntil())
              .contains(Instant.parse("2026-11-16T09:00:00Z"));
        });
    // a forfeited credit
    assertThat(withSessions(3, Optional.of(EXPERT_2), List.of(), 1, NOW).refundIfCancelledNow())
        .hasValueSatisfying(
            refund -> assertThat(refund.reason()).contains(NonRefundableReason.CREDIT_FORFEITED));
  }

  /**
   * Spec 05, decision 13: completed at the end of the last appointment of a fully booked case, not
   * at its start.
   */
  @Test
  void aCaseIsCompletedOnceItsLastAppointmentHasEnded() {
    List<Appointment> fullyBooked = List.of(MONDAY, TUESDAY, WEDNESDAY);
    CustomerConsultationCase whileTheLastRuns =
        withAppointments(fullyBooked, Instant.parse("2026-11-18T09:30:00Z"));
    CustomerConsultationCase atItsEnd = withAppointments(fullyBooked, WEDNESDAY.end());

    assertThat(whileTheLastRuns.canCancel()).isTrue();
    assertThat(whileTheLastRuns.refundIfCancelledNow())
        .hasValueSatisfying(
            refund -> assertThat(refund.reason()).contains(NonRefundableReason.SESSION_TOOK_PLACE));
    assertThat(atItsEnd.canCancel()).isFalse();
    assertThat(atItsEnd.refundIfCancelledNow()).isEmpty();
    assertThat(atItsEnd.canBook()).isFalse();
  }

  @Test
  void aCaseWhoseEveryCreditWasForfeitedIsCompleted() {
    CustomerConsultationCase consultationCase =
        withSessions(1, Optional.of(EXPERT_2), List.of(), 1, NOW);

    assertThat(consultationCase.canCancel()).isFalse();
    assertThat(consultationCase.refundIfCancelledNow()).isEmpty();
  }

  /**
   * Spec 05, decision 6: a cancelled case can be neither booked nor cancelled again, whatever its
   * credits.
   */
  @Test
  void aCancelledCaseCanBeNeitherBookedNorCancelled() {
    CustomerConsultationCase consultationCase =
        cancelled(List.of(MONDAY), Instant.parse("2026-11-16T12:00:00Z"));

    assertThat(consultationCase.cancellation())
        .contains(new CancellationOutcome(Instant.parse("2026-11-15T11:00:00Z"), false));
    assertThat(consultationCase.openCredits()).isEqualTo(2);
    assertThat(consultationCase.canBook()).isFalse();
    assertThat(consultationCase.canCancel()).isFalse();
    assertThat(consultationCase.refundIfCancelledNow()).isEmpty();
    assertThat(consultationCase.pastAppointments())
        .extracting(CustomerAppointment::id)
        .containsExactly(MONDAY.id());
  }

  /**
   * The preview is present exactly when the case can be cancelled, at every time and in every
   * state.
   */
  @Test
  void theRefundPreviewIsPresentExactlyWhenTheCaseCanBeCancelled() {
    List<CustomerConsultationCase> cases =
        List.of(
            withSessions(3),
            withSessions(1, Optional.of(EXPERT_2), List.of(), 1, NOW),
            cancelled(List.of(), NOW),
            cancelled(List.of(TUESDAY), TUESDAY.start()),
            withAppointments(List.of(MONDAY, TUESDAY, WEDNESDAY), NOW),
            withAppointments(
                List.of(MONDAY, TUESDAY, WEDNESDAY), Instant.parse("2026-11-16T10:00:00Z")),
            withAppointments(List.of(MONDAY, TUESDAY, WEDNESDAY), WEDNESDAY.start()),
            withAppointments(List.of(MONDAY, TUESDAY, WEDNESDAY), WEDNESDAY.end()));
    for (CustomerConsultationCase consultationCase : cases) {
      assertThat(consultationCase.refundIfCancelledNow().isPresent())
          .as(consultationCase.toString())
          .isEqualTo(consultationCase.canCancel());
    }
  }
}
