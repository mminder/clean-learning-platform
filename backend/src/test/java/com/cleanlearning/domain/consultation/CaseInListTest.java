package com.cleanlearning.domain.consultation;

import static org.assertj.core.api.Assertions.assertThat;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseGroup;
import com.cleanlearning.domain.offering.value.SessionCount;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * A case in a "My …" list at a given time (D-17, spec 04 decisions 6 and 21, spec 05 decisions 6
 * and 8): its group and the order of the list. Every booked appointment uses a credit, consumed or
 * not, and the appointments' states are {@link Appointment}'s, the same as the case detail's.
 * Instants are in UTC; Zurich is UTC+1 in November.
 */
class CaseInListTest {

  /** Mon 9 Nov 2026, 15:00 in Zurich. */
  private static final Instant NOW = Instant.parse("2026-11-09T14:00:00Z");

  /** Mon 9 Nov 2026, 14:03 in Zurich: when most cases were bought. */
  private static final Instant PURCHASED = Instant.parse("2026-11-09T13:03:12.345Z");

  /** Tue 17 Nov 2026, 10:00–11:00 in Zurich. */
  private static final Instant START = Instant.parse("2026-11-17T09:00:00Z");

  private static final Instant END = Instant.parse("2026-11-17T10:00:00Z");

  /** An active case with 60-minute appointments booked at these starts. */
  private static CaseInList listed(
      int sessionCount, Instant purchasedAt, String... startsOfAppointments) {
    return listed(
        new ConsultationCaseId(UUID.randomUUID()),
        sessionCount,
        0,
        purchasedAt,
        Optional.empty(),
        startsOfAppointments);
  }

  /** A case with this many credits forfeited, by appointments cancelled late. */
  private static CaseInList withForfeited(
      int sessionCount, int forfeitedCredits, Instant purchasedAt, String... startsOfAppointments) {
    return listed(
        new ConsultationCaseId(UUID.randomUUID()),
        sessionCount,
        forfeitedCredits,
        purchasedAt,
        Optional.empty(),
        startsOfAppointments);
  }

  /**
   * A case cancelled at {@code cancelledAt}; it keeps whatever appointments and credits the test
   * gives it.
   */
  private static CaseInList cancelled(
      int sessionCount, Instant purchasedAt, Instant cancelledAt, String... startsOfAppointments) {
    return listed(
        new ConsultationCaseId(UUID.randomUUID()),
        sessionCount,
        0,
        purchasedAt,
        Optional.of(cancelledAt),
        startsOfAppointments);
  }

  /** An active case with the appointment on Tue 17 Nov 2026, 10:00–11:00. */
  private static CaseInList withAppointment(int sessionCount) {
    return listed(sessionCount, PURCHASED, START.toString());
  }

  private static CaseInList listed(
      ConsultationCaseId id,
      int sessionCount,
      int forfeitedCredits,
      Instant purchasedAt,
      Optional<Instant> cancelledAt,
      String... startsOfAppointments) {
    List<Appointment> booked =
        Arrays.stream(startsOfAppointments)
            .map(Instant::parse)
            .map(start ->
                new Appointment(
                    new AppointmentId(UUID.randomUUID()), id, start, start.plusSeconds(3_600)))
            .toList();
    List<CancelledAppointment> cancelledLate =
        Stream.generate(() ->
                new CancelledAppointment(new AppointmentId(UUID.randomUUID()), START, END, false))
            .limit(forfeitedCredits)
            .toList();
    return new CaseInList(
        id,
        purchasedAt,
        cancelledAt,
        new CaseCredits(new SessionCount(sessionCount), booked, cancelledLate));
  }

  private static List<CaseInList> sorted(Instant asOf, CaseInList... cases) {
    return Arrays.stream(cases).sorted(CaseInList.orderAt(asOf)).toList();
  }

  @Test
  void aCaseWithoutAppointmentsAndWithOpenCreditsNeedsBooking() {
    assertThat(listed(3, PURCHASED).groupAt(NOW)).isEqualTo(PurchaseGroup.NEEDS_BOOKING);
  }

  @Test
  void aCaseWithAnUpcomingAppointmentIsUpcomingAlsoWithOpenCredits() {
    assertThat(withAppointment(3).groupAt(NOW)).isEqualTo(PurchaseGroup.UPCOMING);
  }

  @Test
  void aCaseWithEveryCreditBookedAndOneAppointmentStillAheadIsUpcoming() {
    CaseInList listed = listed(2, PURCHASED, "2026-11-16T09:00:00Z", "2026-11-17T09:00:00Z");

    assertThat(listed.credits().openCredits()).isZero();
    assertThat(listed.groupAt(Instant.parse("2026-11-17T09:30:00Z")))
        .isEqualTo(PurchaseGroup.UPCOMING);
  }

  /**
   * Spec 04, decision 21: consumed from its start, upcoming until its end, so its case is Upcoming
   * until then.
   */
  @Test
  void aCaseIsUpcomingFromItsAppointmentsStartUntilItsEnd() {
    assertThat(withAppointment(1).groupAt(START.minusSeconds(1))).isEqualTo(PurchaseGroup.UPCOMING);
    assertThat(withAppointment(1).groupAt(START)).isEqualTo(PurchaseGroup.UPCOMING);
    assertThat(withAppointment(1).groupAt(END.minusSeconds(1))).isEqualTo(PurchaseGroup.UPCOMING);
    assertThat(withAppointment(1).groupAt(END)).isEqualTo(PurchaseGroup.PAST);
  }

  @Test
  void aCaseWithPastAppointmentsAndAnOpenCreditNeedsBooking() {
    assertThat(withAppointment(3).groupAt(END)).isEqualTo(PurchaseGroup.NEEDS_BOOKING);
    assertThat(withAppointment(3).credits().openCredits()).isEqualTo(2);
  }

  @Test
  void aCaseWhoseCreditsAreAllBookedAndEveryAppointmentHasEndedIsPast() {
    CaseInList listed = listed(2, PURCHASED, "2026-11-16T09:00:00Z", "2026-11-17T09:00:00Z");

    assertThat(listed.groupAt(Instant.parse("2026-11-18T09:00:00Z"))).isEqualTo(PurchaseGroup.PAST);
    assertThat(listed.credits().openCredits()).isZero();
  }

  /** Spec 05: a forfeited credit is used, as a booked one is; one cancelled in time is neither. */
  @Test
  void aCaseWithAForfeitedCreditAndAnOpenOneNeedsBooking() {
    // one consumed, one cancelled in time, one late
    CaseInList listed = withForfeited(3, 1, PURCHASED, "2026-11-17T09:00:00Z");

    assertThat(listed.credits().openCredits()).isOne();
    assertThat(listed.groupAt(Instant.parse("2026-11-18T09:00:00Z")))
        .isEqualTo(PurchaseGroup.NEEDS_BOOKING);
  }

  /**
   * Spec 05: two consumed appointments and one cancelled late leave no open credit, so the case is
   * Past.
   */
  @Test
  void aForfeitedCreditCanCompleteTheCase() {
    CaseInList listed =
        withForfeited(3, 1, PURCHASED, "2026-11-16T09:00:00Z", "2026-11-17T09:00:00Z");

    assertThat(listed.credits().openCredits()).isZero();
    assertThat(listed.groupAt(Instant.parse("2026-11-18T09:00:00Z"))).isEqualTo(PurchaseGroup.PAST);
  }

  @Test
  void aCaseWhoseEveryCreditWasForfeitedIsPastWithoutAnAppointment() {
    CaseInList listed = withForfeited(1, 1, PURCHASED);

    assertThat(listed.credits().openCredits()).isZero();
    assertThat(listed.groupAt(NOW)).isEqualTo(PurchaseGroup.PAST);
  }

  /**
   * Spec 05, decision 6: a cancelled case is Past, whatever it had: open credits, an upcoming or a
   * running one.
   */
  @Test
  void aCancelledCaseIsPastWhateverItHad() {
    Instant cancelledAt = Instant.parse("2026-11-09T13:30:00Z");

    assertThat(cancelled(3, PURCHASED, cancelledAt).groupAt(NOW)).isEqualTo(PurchaseGroup.PAST);
    assertThat(cancelled(3, PURCHASED, cancelledAt, "2026-11-17T09:00:00Z").groupAt(NOW))
        .isEqualTo(PurchaseGroup.PAST);
    // running at asOf
    assertThat(
            cancelled(3, PURCHASED, cancelledAt, "2026-11-17T09:00:00Z")
                .groupAt(Instant.parse("2026-11-17T09:30:00Z")))
        .isEqualTo(PurchaseGroup.PAST);
    assertThat(cancelled(1, PURCHASED, cancelledAt).credits().openCredits()).isOne();
  }

  @Test
  void theListIsUpcomingThenNeedsBookingThenPast() {
    CaseInList past = listed(1, PURCHASED, "2026-11-17T09:00:00Z");
    CaseInList needsBooking = listed(3, PURCHASED.minusSeconds(86_400));
    CaseInList upcoming = listed(1, PURCHASED.plusSeconds(86_400), "2026-11-19T09:00:00Z");

    assertThat(sorted(Instant.parse("2026-11-18T09:00:00Z"), past, needsBooking, upcoming))
        .containsExactly(upcoming, needsBooking, past);
  }

  @Test
  void upcomingComesByTheNextAppointmentSoonestFirstNotByPurchase() {
    // bought first, but its next appointment is the later one; the other has a past appointment,
    // too
    CaseInList later = listed(3, PURCHASED.minusSeconds(86_400), "2026-11-21T09:00:00Z");
    CaseInList sooner = listed(3, PURCHASED, "2026-11-17T09:00:00Z", "2026-11-19T09:00:00Z");

    assertThat(sorted(Instant.parse("2026-11-18T09:30:00Z"), later, sooner))
        .containsExactly(sooner, later);
  }

  @Test
  void needsBookingComesByPurchaseTimeOldestFirst() {
    CaseInList old = listed(3, PURCHASED.minusSeconds(86_400));
    CaseInList recent = listed(3, PURCHASED);
    CaseInList newest = listed(3, PURCHASED.plusSeconds(3_600));

    assertThat(sorted(NOW, newest, old, recent)).containsExactly(old, recent, newest);
  }

  @Test
  void pastComesByTheLastAppointmentNewestFirst() {
    // the one bought last ended first
    CaseInList endedFirst =
        listed(2, PURCHASED.plusSeconds(86_400), "2026-11-16T09:00:00Z", "2026-11-17T09:00:00Z");
    CaseInList endedLast = listed(1, PURCHASED, "2026-11-20T09:00:00Z");
    CaseInList endedBetween =
        listed(2, PURCHASED.minusSeconds(86_400), "2026-11-10T09:00:00Z", "2026-11-18T09:00:00Z");

    assertThat(sorted(Instant.parse("2026-11-25T09:00:00Z"), endedFirst, endedBetween, endedLast))
        .containsExactly(endedLast, endedBetween, endedFirst);
  }

  /**
   * Spec 05, decision 8: a past case without an appointment sorts by its purchase time, among the
   * others.
   */
  @Test
  void pastComesByThePurchaseTimeWithoutAnAppointment() {
    CaseInList endedOnTheSeventeenth = listed(1, PURCHASED, "2026-11-17T09:00:00Z");
    CaseInList boughtOnTheTwentieth = withForfeited(1, 1, Instant.parse("2026-11-20T09:00:00Z"));
    CaseInList boughtOnTheTenth = withForfeited(1, 1, Instant.parse("2026-11-10T09:00:00Z"));

    assertThat(
            sorted(
                Instant.parse("2026-11-25T09:00:00Z"),
                boughtOnTheTenth,
                endedOnTheSeventeenth,
                boughtOnTheTwentieth))
        .containsExactly(boughtOnTheTwentieth, endedOnTheSeventeenth, boughtOnTheTenth);
  }

  /**
   * Spec 05, decision 8: Past newest first, a completed case by its last appointment, a case
   * without one by its purchase time, a cancelled one by its cancellation time, all in one order.
   */
  @Test
  void pastComesByTheCancellationTimeForACancelledCaseAmongTheOthers() {
    CaseInList endedOnTheSeventeenth = listed(1, PURCHASED, "2026-11-17T09:00:00Z");
    CaseInList boughtOnTheTwentieth = withForfeited(1, 1, Instant.parse("2026-11-20T09:00:00Z"));
    // bought long before, cancelled last: the cancellation, not the purchase or the appointment,
    // decides
    CaseInList cancelledOnTheTwentySecond =
        cancelled(
            3,
            PURCHASED.minusSeconds(864_000),
            Instant.parse("2026-11-22T09:00:00Z"),
            "2026-11-30T09:00:00Z");
    CaseInList cancelledOnTheNineteenth =
        cancelled(1, Instant.parse("2026-11-21T09:00:00Z"), Instant.parse("2026-11-19T09:00:00Z"));

    assertThat(
            sorted(
                Instant.parse("2026-11-25T09:00:00Z"),
                endedOnTheSeventeenth,
                cancelledOnTheNineteenth,
                boughtOnTheTwentieth,
                cancelledOnTheTwentySecond))
        .containsExactly(
            cancelledOnTheTwentySecond,
            boughtOnTheTwentieth,
            cancelledOnTheNineteenth,
            endedOnTheSeventeenth);
  }

  @Test
  void aCancelledCaseComesAfterTheActiveGroups() {
    CaseInList upcoming = withAppointment(3);
    CaseInList needsBooking = listed(3, PURCHASED);
    CaseInList cancelled =
        cancelled(3, PURCHASED.minusSeconds(3_600), Instant.parse("2026-11-09T13:30:00Z"));

    assertThat(sorted(NOW, cancelled, needsBooking, upcoming))
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
    CaseInList running = cancelled(3, PURCHASED, START.plusSeconds(600), START.toString());
    CaseInList cancelledEarlier = cancelled(3, PURCHASED, START.minusSeconds(86_400));
    CaseInList upcoming = listed(3, PURCHASED, "2026-11-18T09:00:00Z");

    assertThat(running.credits().nextAppointment(asOf)).isPresent();
    assertThat(sorted(asOf, cancelledEarlier, running, upcoming))
        .containsExactly(upcoming, running, cancelledEarlier);
  }

  @Test
  void casesWithTheSameKeyComeById() {
    // as Postgres orders a uuid: a leading "b" comes after a leading "3", though the UUID's own
    // order is signed
    CaseInList first =
        listed(
            new ConsultationCaseId(UUID.fromString("34234457-9d51-45b5-a93f-7038a7d9dc11")),
            3,
            0,
            PURCHASED,
            Optional.empty());
    CaseInList second =
        listed(
            new ConsultationCaseId(UUID.fromString("bcbee9df-29af-4797-bc78-6ff628fe303d")),
            3,
            0,
            PURCHASED,
            Optional.empty());

    assertThat(sorted(NOW, second, first)).containsExactly(first, second);
  }
}
