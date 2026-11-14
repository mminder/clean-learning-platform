package com.cleanlearning.domain.consultation.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.cleanlearning.domain.consultation.Appointment;
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
 * A case on "My purchases" at a given time (UC-05, D-17, spec 04 decisions 6 and 21): its group,
 * its next appointment, its credits and the order of the list. Every booked appointment uses a
 * credit, consumed or not, and the appointments' states are {@link Appointment}'s, the same as the
 * case detail's. Instants are in UTC; Zurich is UTC+1 in November.
 */
class CustomerConsultationCaseSummaryTest {

  /** Mon 9 Nov 2026, 15:00 in Zurich. */
  private static final Instant NOW = Instant.parse("2026-11-09T14:00:00Z");

  /** Mon 9 Nov 2026, 14:03 in Zurich: when most cases were bought. */
  private static final Instant PURCHASED = Instant.parse("2026-11-09T13:03:12.345Z");

  /** Tue 17 Nov 2026, 10:00–11:00 in Zurich. */
  private static final Instant START = Instant.parse("2026-11-17T09:00:00Z");

  private static final Instant END = Instant.parse("2026-11-17T10:00:00Z");

  private static Appointment appointment(ConsultationCaseId id, String start) {
    Instant instant = Instant.parse(start);
    return new Appointment(
        new AppointmentId(UUID.randomUUID()), id, instant, instant.plusSeconds(3_600));
  }

  private static CustomerConsultationCaseSummary summary(
      int sessionCount, Instant purchasedAt, Instant asOf, String... startsOfAppointments) {
    ConsultationCaseId id = new ConsultationCaseId(UUID.randomUUID());
    return summary(id, sessionCount, purchasedAt, asOf, startsOfAppointments);
  }

  private static CustomerConsultationCaseSummary summary(
      ConsultationCaseId id,
      int sessionCount,
      Instant purchasedAt,
      Instant asOf,
      String... startsOfAppointments) {
    return withForfeited(id, sessionCount, 0, purchasedAt, asOf, startsOfAppointments);
  }

  /**
   * A case with this many credits forfeited, by appointments cancelled late, which it doesn't list.
   */
  private static CustomerConsultationCaseSummary withForfeited(
      int sessionCount,
      int forfeitedCredits,
      Instant purchasedAt,
      Instant asOf,
      String... startsOfAppointments) {
    return withForfeited(
        new ConsultationCaseId(UUID.randomUUID()),
        sessionCount,
        forfeitedCredits,
        purchasedAt,
        asOf,
        startsOfAppointments);
  }

  private static CustomerConsultationCaseSummary withForfeited(
      ConsultationCaseId id,
      int sessionCount,
      int forfeitedCredits,
      Instant purchasedAt,
      Instant asOf,
      String... startsOfAppointments) {
    List<Appointment> appointments =
        Arrays.stream(startsOfAppointments).map(start -> appointment(id, start)).toList();
    return new CustomerConsultationCaseSummary(
        id,
        new OfferingName("Career Coaching"),
        Optional.empty(),
        new SessionCount(sessionCount),
        purchasedAt,
        Optional.of(new DisplayName("Expert 2")),
        appointments,
        forfeitedCredits,
        Optional.empty(),
        asOf);
  }

  /**
   * A case cancelled at {@code cancelledAt}; it keeps whatever appointments and credits the test
   * gives it.
   */
  private static CustomerConsultationCaseSummary cancelled(
      int sessionCount,
      Instant purchasedAt,
      Instant cancelledAt,
      Instant asOf,
      String... startsOfAppointments) {
    ConsultationCaseId id = new ConsultationCaseId(UUID.randomUUID());
    List<Appointment> appointments =
        Arrays.stream(startsOfAppointments).map(start -> appointment(id, start)).toList();
    return new CustomerConsultationCaseSummary(
        id,
        new OfferingName("Career Coaching"),
        Optional.empty(),
        new SessionCount(sessionCount),
        purchasedAt,
        Optional.of(new DisplayName("Expert 2")),
        appointments,
        0,
        Optional.of(cancelledAt),
        asOf);
  }

  private static CustomerConsultationCaseSummary withAppointment(int sessionCount, Instant asOf) {
    ConsultationCaseId id = new ConsultationCaseId(UUID.randomUUID());
    return new CustomerConsultationCaseSummary(
        id,
        new OfferingName("Career Coaching"),
        Optional.empty(),
        new SessionCount(sessionCount),
        PURCHASED,
        Optional.of(new DisplayName("Expert 2")),
        List.of(new Appointment(new AppointmentId(UUID.randomUUID()), id, START, END)),
        0,
        Optional.empty(),
        asOf);
  }

  @Test
  void totalCreditsAreTheSessionCount() {
    assertThat(summary(3, PURCHASED, NOW).totalCredits()).isEqualTo(3);
    assertThat(summary(5, PURCHASED, NOW).totalCredits()).isEqualTo(5);
  }

  @Test
  void everyBookedAppointmentUsesACreditConsumedOrNot() {
    assertThat(summary(3, PURCHASED, NOW).openCredits()).isEqualTo(3);
    assertThat(summary(3, PURCHASED, NOW, "2026-11-17T09:00:00Z").openCredits()).isEqualTo(2);
    assertThat(
            summary(
                    3,
                    PURCHASED,
                    Instant.parse("2026-11-20T09:00:00Z"),
                    "2026-11-17T09:00:00Z",
                    "2026-11-18T09:00:00Z")
                .openCredits())
        .isEqualTo(1);
    assertThat(summary(1, PURCHASED, NOW, "2026-11-17T09:00:00Z").openCredits()).isZero();
  }

  @Test
  void aCaseWithoutAppointmentsAndWithOpenCreditsNeedsBooking() {
    CustomerConsultationCaseSummary summary = summary(3, PURCHASED, NOW);

    assertThat(summary.group()).isEqualTo(PurchaseGroup.NEEDS_BOOKING);
    assertThat(summary.nextAppointment()).isEmpty();
  }

  @Test
  void aCaseWithAnUpcomingAppointmentIsUpcomingAlsoWithOpenCredits() {
    CustomerConsultationCaseSummary summary = withAppointment(3, NOW);

    assertThat(summary.group()).isEqualTo(PurchaseGroup.UPCOMING);
    assertThat(summary.nextAppointment())
        .hasValueSatisfying(next -> {
          assertThat(next.start()).isEqualTo(START);
          assertThat(next.end()).isEqualTo(END);
          assertThat(next.status()).isEqualTo(AppointmentStatus.BOOKED);
        });
  }

  @Test
  void aCaseWithEveryCreditBookedAndOneAppointmentStillAheadIsUpcoming() {
    CustomerConsultationCaseSummary summary =
        summary(
            2,
            PURCHASED,
            Instant.parse("2026-11-17T09:30:00Z"),
            "2026-11-16T09:00:00Z",
            "2026-11-17T09:00:00Z");

    assertThat(summary.openCredits()).isZero();
    assertThat(summary.group()).isEqualTo(PurchaseGroup.UPCOMING);
  }

  /**
   * Spec 04, decision 21: consumed from its start, upcoming until its end, so its case is Upcoming
   * until then.
   */
  @Test
  void aCaseIsUpcomingFromItsAppointmentsStartUntilItsEnd() {
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
  void aCaseWithPastAppointmentsAndAnOpenCreditNeedsBooking() {
    assertThat(withAppointment(3, END).group()).isEqualTo(PurchaseGroup.NEEDS_BOOKING);
    assertThat(withAppointment(3, END).nextAppointment()).isEmpty();
    assertThat(withAppointment(3, END).openCredits()).isEqualTo(2);
  }

  @Test
  void aCaseWhoseCreditsAreAllBookedAndEveryAppointmentHasEndedIsPast() {
    CustomerConsultationCaseSummary summary =
        summary(
            2,
            PURCHASED,
            Instant.parse("2026-11-18T09:00:00Z"),
            "2026-11-16T09:00:00Z",
            "2026-11-17T09:00:00Z");

    assertThat(summary.group()).isEqualTo(PurchaseGroup.PAST);
    assertThat(summary.openCredits()).isZero();
    assertThat(summary.nextAppointment()).isEmpty();
  }

  @Test
  void theNextAppointmentIsTheSoonestUpcomingOne() {
    CustomerConsultationCaseSummary summary =
        summary(
            3,
            PURCHASED,
            NOW,
            "2026-11-19T09:00:00Z",
            "2026-11-17T09:00:00Z",
            "2026-11-18T09:00:00Z");

    assertThat(summary.nextAppointment().orElseThrow().start()).isEqualTo(START);
  }

  @Test
  void theNextAppointmentSkipsThePastOnes() {
    CustomerConsultationCaseSummary summary =
        summary(
            3,
            PURCHASED,
            Instant.parse("2026-11-18T09:30:00Z"),
            "2026-11-17T09:00:00Z",
            "2026-11-19T09:00:00Z");

    assertThat(summary.nextAppointment().orElseThrow().start())
        .isEqualTo(Instant.parse("2026-11-19T09:00:00Z"));
  }

  @Test
  void theListIsUpcomingThenNeedsBookingThenPast() {
    CustomerConsultationCaseSummary past =
        summary(1, PURCHASED, Instant.parse("2026-11-18T09:00:00Z"), "2026-11-17T09:00:00Z");
    CustomerConsultationCaseSummary needsBooking =
        summary(3, PURCHASED.minusSeconds(86_400), Instant.parse("2026-11-18T09:00:00Z"));
    CustomerConsultationCaseSummary upcoming =
        summary(
            1,
            PURCHASED.plusSeconds(86_400),
            Instant.parse("2026-11-18T09:00:00Z"),
            "2026-11-19T09:00:00Z");

    assertThat(
            List.of(past, needsBooking, upcoming).stream()
                .sorted(CustomerConsultationCaseSummary.MY_PURCHASES_ORDER)
                .toList())
        .containsExactly(upcoming, needsBooking, past);
  }

  @Test
  void upcomingComesByTheNextAppointmentSoonestFirstNotByPurchase() {
    Instant asOf = Instant.parse("2026-11-18T09:30:00Z");
    // bought first, but its next appointment is the later one; the other has a past appointment,
    // too
    CustomerConsultationCaseSummary later =
        summary(3, PURCHASED.minusSeconds(86_400), asOf, "2026-11-21T09:00:00Z");
    CustomerConsultationCaseSummary sooner =
        summary(3, PURCHASED, asOf, "2026-11-17T09:00:00Z", "2026-11-19T09:00:00Z");

    assertThat(
            List.of(later, sooner).stream()
                .sorted(CustomerConsultationCaseSummary.MY_PURCHASES_ORDER)
                .toList())
        .containsExactly(sooner, later);
  }

  @Test
  void needsBookingComesByPurchaseTimeOldestFirst() {
    CustomerConsultationCaseSummary old = summary(3, PURCHASED.minusSeconds(86_400), NOW);
    CustomerConsultationCaseSummary recent = summary(3, PURCHASED, NOW);
    CustomerConsultationCaseSummary newest = summary(3, PURCHASED.plusSeconds(3_600), NOW);

    assertThat(
            List.of(newest, old, recent).stream()
                .sorted(CustomerConsultationCaseSummary.MY_PURCHASES_ORDER)
                .toList())
        .containsExactly(old, recent, newest);
  }

  @Test
  void pastComesByTheLastAppointmentNewestFirst() {
    Instant asOf = Instant.parse("2026-11-25T09:00:00Z");
    // the one bought last ended first
    CustomerConsultationCaseSummary endedFirst =
        summary(
            2, PURCHASED.plusSeconds(86_400), asOf, "2026-11-16T09:00:00Z", "2026-11-17T09:00:00Z");
    CustomerConsultationCaseSummary endedLast = summary(1, PURCHASED, asOf, "2026-11-20T09:00:00Z");
    CustomerConsultationCaseSummary endedBetween =
        summary(
            2,
            PURCHASED.minusSeconds(86_400),
            asOf,
            "2026-11-10T09:00:00Z",
            "2026-11-18T09:00:00Z");

    assertThat(
            List.of(endedFirst, endedBetween, endedLast).stream()
                .sorted(CustomerConsultationCaseSummary.MY_PURCHASES_ORDER)
                .toList())
        .containsExactly(endedLast, endedBetween, endedFirst);
  }

  /** Spec 05: a forfeited credit is used, as a booked one is; one cancelled in time is neither. */
  @Test
  void aForfeitedCreditIsNotOpen() {
    assertThat(withForfeited(3, 1, PURCHASED, NOW).openCredits()).isEqualTo(2);
    // one consumed, one cancelled in time, one late
    assertThat(
            withForfeited(
                    3, 1, PURCHASED, Instant.parse("2026-11-18T09:00:00Z"), "2026-11-17T09:00:00Z")
                .openCredits())
        .isOne();
    assertThat(
            withForfeited(
                    3, 1, PURCHASED, Instant.parse("2026-11-18T09:00:00Z"), "2026-11-17T09:00:00Z")
                .group())
        .isEqualTo(PurchaseGroup.NEEDS_BOOKING);
  }

  /**
   * Spec 05: two consumed appointments and one cancelled late leave no open credit, so the case is
   * Past.
   */
  @Test
  void aForfeitedCreditCanCompleteTheCase() {
    CustomerConsultationCaseSummary summary =
        withForfeited(
            3,
            1,
            PURCHASED,
            Instant.parse("2026-11-18T09:00:00Z"),
            "2026-11-16T09:00:00Z",
            "2026-11-17T09:00:00Z");

    assertThat(summary.openCredits()).isZero();
    assertThat(summary.group()).isEqualTo(PurchaseGroup.PAST);
  }

  @Test
  void aCaseWhoseEveryCreditWasForfeitedIsPastWithoutAnAppointment() {
    CustomerConsultationCaseSummary summary = withForfeited(1, 1, PURCHASED, NOW);

    assertThat(summary.openCredits()).isZero();
    assertThat(summary.nextAppointment()).isEmpty();
    assertThat(summary.group()).isEqualTo(PurchaseGroup.PAST);
  }

  /**
   * Spec 05, decision 8: a past case without an appointment sorts by its purchase time, among the
   * others.
   */
  @Test
  void pastComesByThePurchaseTimeWithoutAnAppointment() {
    Instant asOf = Instant.parse("2026-11-25T09:00:00Z");
    CustomerConsultationCaseSummary endedOnTheSeventeenth =
        summary(1, PURCHASED, asOf, "2026-11-17T09:00:00Z");
    CustomerConsultationCaseSummary boughtOnTheTwentieth =
        withForfeited(1, 1, Instant.parse("2026-11-20T09:00:00Z"), asOf);
    CustomerConsultationCaseSummary boughtOnTheTenth =
        withForfeited(1, 1, Instant.parse("2026-11-10T09:00:00Z"), asOf);

    assertThat(
            List.of(boughtOnTheTenth, endedOnTheSeventeenth, boughtOnTheTwentieth).stream()
                .sorted(CustomerConsultationCaseSummary.MY_PURCHASES_ORDER)
                .toList())
        .containsExactly(boughtOnTheTwentieth, endedOnTheSeventeenth, boughtOnTheTenth);
  }

  /**
   * Spec 05, decision 6: a cancelled case is Past, whatever it had: open credits, an upcoming or a
   * running one.
   */
  @Test
  void aCancelledCaseIsPastWhateverItHad() {
    Instant cancelledAt = Instant.parse("2026-11-09T13:30:00Z");

    assertThat(cancelled(3, PURCHASED, cancelledAt, NOW).group()).isEqualTo(PurchaseGroup.PAST);
    assertThat(cancelled(3, PURCHASED, cancelledAt, NOW, "2026-11-17T09:00:00Z").group())
        .isEqualTo(PurchaseGroup.PAST);
    // running at asOf
    assertThat(
            cancelled(
                    3,
                    PURCHASED,
                    cancelledAt,
                    Instant.parse("2026-11-17T09:30:00Z"),
                    "2026-11-17T09:00:00Z")
                .group())
        .isEqualTo(PurchaseGroup.PAST);
    assertThat(cancelled(1, PURCHASED, cancelledAt, NOW).openCredits()).isOne();
  }

  /**
   * Spec 05, decision 8: Past newest first, a completed case by its last appointment, a case
   * without one by its purchase time, a cancelled one by its cancellation time, all in one order.
   */
  @Test
  void pastComesByTheCancellationTimeForACancelledCaseAmongTheOthers() {
    Instant asOf = Instant.parse("2026-11-25T09:00:00Z");
    CustomerConsultationCaseSummary endedOnTheSeventeenth =
        summary(1, PURCHASED, asOf, "2026-11-17T09:00:00Z");
    CustomerConsultationCaseSummary boughtOnTheTwentieth =
        withForfeited(1, 1, Instant.parse("2026-11-20T09:00:00Z"), asOf);
    // bought long before, cancelled last: the cancellation, not the purchase or the appointment,
    // decides
    CustomerConsultationCaseSummary cancelledOnTheTwentySecond =
        cancelled(
            3,
            PURCHASED.minusSeconds(864_000),
            Instant.parse("2026-11-22T09:00:00Z"),
            asOf,
            "2026-11-30T09:00:00Z");
    CustomerConsultationCaseSummary cancelledOnTheNineteenth =
        cancelled(
            1, Instant.parse("2026-11-21T09:00:00Z"), Instant.parse("2026-11-19T09:00:00Z"), asOf);

    assertThat(
            List.of(
                    endedOnTheSeventeenth,
                    cancelledOnTheNineteenth,
                    boughtOnTheTwentieth,
                    cancelledOnTheTwentySecond)
                .stream()
                .sorted(CustomerConsultationCaseSummary.MY_PURCHASES_ORDER)
                .toList())
        .containsExactly(
            cancelledOnTheTwentySecond,
            boughtOnTheTwentieth,
            cancelledOnTheNineteenth,
            endedOnTheSeventeenth);
  }

  @Test
  void aCancelledCaseComesAfterTheActiveGroups() {
    CustomerConsultationCaseSummary upcoming = withAppointment(3, NOW);
    CustomerConsultationCaseSummary needsBooking = summary(3, PURCHASED, NOW);
    CustomerConsultationCaseSummary cancelled =
        cancelled(3, PURCHASED.minusSeconds(3_600), Instant.parse("2026-11-09T13:30:00Z"), NOW);

    assertThat(
            List.of(cancelled, needsBooking, upcoming).stream()
                .sorted(CustomerConsultationCaseSummary.MY_PURCHASES_ORDER)
                .toList())
        .containsExactly(upcoming, needsBooking, cancelled);
  }

  /**
   * Spec 05, decision 6: a running appointment isn't cancelled with its case, and stays its next
   * appointment; the case is still Past, by its cancellation time, among the cases of the other
   * groups.
   */
  @Test
  void aCancelledCaseWithARunningAppointmentIsPastByItsCancellationTime() {
    Instant asOf = START.plusSeconds(1_800);
    CustomerConsultationCaseSummary running =
        cancelled(3, PURCHASED, START.plusSeconds(600), asOf, START.toString());
    CustomerConsultationCaseSummary cancelledEarlier =
        cancelled(3, PURCHASED, START.minusSeconds(86_400), asOf);
    CustomerConsultationCaseSummary upcoming = summary(3, PURCHASED, asOf, "2026-11-18T09:00:00Z");

    assertThat(running.nextAppointment()).isPresent();
    assertThat(
            List.of(cancelledEarlier, running, upcoming).stream()
                .sorted(CustomerConsultationCaseSummary.MY_PURCHASES_ORDER)
                .toList())
        .containsExactly(upcoming, running, cancelledEarlier);
  }

  @Test
  void casesWithTheSameKeyComeById() {
    // as Postgres orders a uuid: a leading "b" comes after a leading "3", though the UUID's own
    // order is signed
    ConsultationCaseId low =
        new ConsultationCaseId(UUID.fromString("34234457-9d51-45b5-a93f-7038a7d9dc11"));
    ConsultationCaseId high =
        new ConsultationCaseId(UUID.fromString("bcbee9df-29af-4797-bc78-6ff628fe303d"));
    CustomerConsultationCaseSummary first = summary(low, 3, PURCHASED, NOW);
    CustomerConsultationCaseSummary second = summary(high, 3, PURCHASED, NOW);

    assertThat(
            List.of(second, first).stream()
                .sorted(CustomerConsultationCaseSummary.MY_PURCHASES_ORDER)
                .toList())
        .containsExactly(first, second);
  }
}
