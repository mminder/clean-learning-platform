package com.cleanlearning.domain.consultation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.value.Money;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The credits a case's booked appointments and its forfeited ones use, when that completes the case
 * (glossary, spec 05 decision 13), and which of its appointments are upcoming and past at a given
 * time (spec 04, decision 21). Instants are in UTC.
 */
class CaseCreditsTest {

  private static final ConsultationCaseId CASE = new ConsultationCaseId(UUID.randomUUID());

  /** Tue 17 Nov 2026, 10:00–11:00 in Zurich. */
  private static final Instant START = Instant.parse("2026-11-17T09:00:00Z");

  private static final Instant END = Instant.parse("2026-11-17T10:00:00Z");

  private static final Appointment TEN_TO_ELEVEN =
      new Appointment(new AppointmentId(UUID.randomUUID()), CASE, START, END);

  private static Appointment appointment(String start, String end) {
    return new Appointment(
        new AppointmentId(UUID.randomUUID()), CASE, Instant.parse(start), Instant.parse(end));
  }

  private static CancelledAppointment cancelled(boolean creditRefunded) {
    return new CancelledAppointment(
        new AppointmentId(UUID.randomUUID()), START, END, creditRefunded);
  }

  /** The credits of a case with these booked appointments and this many cancelled late. */
  private static CaseCredits credits(int sessionCount, List<Appointment> booked, int forfeited) {
    return new CaseCredits(
        new SessionCount(sessionCount),
        booked,
        Stream.generate(() -> cancelled(false)).limit(forfeited).toList());
  }

  /** The credits of a case with this many booked appointments and this many cancelled late. */
  private static CaseCredits credits(int sessionCount, int booked, int forfeited) {
    return credits(
        sessionCount, Stream.generate(() -> TEN_TO_ELEVEN).limit(booked).toList(), forfeited);
  }

  @Test
  void theTotalCreditsAreTheSessionCount() {
    assertThat(credits(3, 0, 0).totalCredits()).isEqualTo(3);
    assertThat(credits(5, 1, 1).totalCredits()).isEqualTo(5);
    assertThat(credits(1, 0, 0).totalCredits()).isOne();
  }

  @Test
  void theOpenCreditsAreTheSessionsLessTheBookedAppointmentsConsumedOrNot() {
    assertThat(credits(3, 0, 0).openCredits()).isEqualTo(3);
    assertThat(credits(3, 2, 0).openCredits()).isEqualTo(1);
    assertThat(credits(3, 3, 0).openCredits()).isZero();
    assertThat(credits(5, 1, 0).openCredits()).isEqualTo(4);
  }

  @Test
  void theOpenCreditsAlsoLackTheForfeitedOnes() {
    assertThat(credits(3, 0, 1).openCredits()).isEqualTo(2);
    // one consumed, one cancelled in time (neither booked nor forfeited), one cancelled late
    assertThat(
            new CaseCredits(
                    new SessionCount(3),
                    List.of(TEN_TO_ELEVEN),
                    List.of(cancelled(true), cancelled(false)))
                .openCredits())
        .isOne();
    assertThat(credits(3, 2, 1).openCredits()).isZero();
    assertThat(credits(3, 0, 3).openCredits()).isZero();
    assertThat(credits(1, 0, 1).openCredits()).isZero();
  }

  @Test
  void theForfeitedCreditsAreTheAppointmentsCancelledLate() {
    assertThat(credits(3, 0, 0).forfeitedCredits()).isZero();
    assertThat(
            new CaseCredits(
                    new SessionCount(3), List.of(), List.of(cancelled(true), cancelled(false)))
                .forfeitedCredits())
        .isOne();
    assertThat(credits(3, 0, 2).forfeitedCredits()).isEqualTo(2);
  }

  @Test
  void anOpenCreditIsLeftWhileNotEveryCreditIsBookedOrForfeited() {
    assertThat(credits(3, 1, 1).hasOpenCredit()).isTrue();
    assertThat(credits(3, 2, 1).hasOpenCredit()).isFalse();
    assertThat(credits(3, 3, 0).hasOpenCredit()).isFalse();
  }

  @Test
  void anAppointmentIsEitherBookedOrCancelledNeverBoth() {
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new CaseCredits(
                new SessionCount(3),
                List.of(TEN_TO_ELEVEN),
                List.of(new CancelledAppointment(TEN_TO_ELEVEN.id(), START, END, true))))
        .withMessage("An appointment is either booked or cancelled, never both.");
  }

  @Test
  void needsItsSessionCount() {
    assertThatNullPointerException()
        .isThrownBy(() -> new CaseCredits(null, List.of(), List.of()))
        .withMessage("sessionCount");
  }

  @Test
  void theCreditsCountFromThePurchaseTermsSessionCount() {
    PurchaseTerms threeSessions =
        new PurchaseTerms(
            Money.chf("450.00"),
            new SessionCount(3),
            new SessionDuration(60),
            new CancellationDeadline(24));

    credits(3, 1, 0).requireSessionCountOf(threeSessions);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> credits(2, 1, 0).requireSessionCountOf(threeSessions))
        .withMessage("A case's credits count from its purchase terms' session count.");
  }

  /**
   * Spec 05, decision 13: completed once no credit is open and the last booked appointment has
   * ended.
   */
  @Test
  void aCaseIsCompletedWithoutOpenCreditsOnceItsLastAppointmentHasEnded() {
    Appointment earlier = appointment("2026-11-16T09:00:00Z", "2026-11-16T10:00:00Z");
    CaseCredits both = credits(2, List.of(TEN_TO_ELEVEN, earlier), 0);

    assertThat(both.isCompletedAt(START.minusSeconds(1))).isFalse();
    // while the last session runs, it isn't completed yet
    assertThat(both.isCompletedAt(START)).isFalse();
    assertThat(both.isCompletedAt(END.minusSeconds(1))).isFalse();
    assertThat(both.isCompletedAt(END)).isTrue();
  }

  @Test
  void aCaseWithAnOpenCreditIsNotCompleted() {
    assertThat(credits(3, List.of(TEN_TO_ELEVEN), 0).isCompletedAt(END)).isFalse();
    assertThat(credits(3, List.of(), 0).isCompletedAt(END)).isFalse();
    // one consumed, one forfeited, one open
    assertThat(credits(3, List.of(TEN_TO_ELEVEN), 1).isCompletedAt(END)).isFalse();
  }

  @Test
  void forfeitedCreditsCanCompleteACase() {
    assertThat(credits(1, List.of(), 1).isCompletedAt(START)).isTrue();
    assertThat(credits(2, List.of(TEN_TO_ELEVEN), 1).isCompletedAt(END)).isTrue();
    assertThat(credits(2, List.of(TEN_TO_ELEVEN), 1).isCompletedAt(START)).isFalse();
  }

  @Test
  void theUpcomingAppointmentsAreTheOnesNotEndedSoonestFirstARunningOneToo() {
    Appointment past = appointment("2026-11-16T09:00:00Z", "2026-11-16T10:00:00Z");
    Appointment later = appointment("2026-11-20T09:00:00Z", "2026-11-20T10:00:00Z");
    Appointment soon = appointment("2026-11-18T09:00:00Z", "2026-11-18T10:00:00Z");
    CaseCredits credits = credits(5, List.of(later, past, soon, TEN_TO_ELEVEN), 0);

    // while the one from 10:00 to 11:00 runs
    assertThat(credits.upcomingAppointments(Instant.parse("2026-11-17T09:30:00Z")))
        .containsExactly(TEN_TO_ELEVEN, soon, later);
    assertThat(credits.upcomingAppointments(END)).containsExactly(soon, later);
  }

  @Test
  void thePastAppointmentsAreTheOnesEndedNewestFirst() {
    Appointment first = appointment("2026-11-16T09:00:00Z", "2026-11-16T10:00:00Z");
    Appointment upcoming = appointment("2026-11-20T09:00:00Z", "2026-11-20T10:00:00Z");
    CaseCredits credits = credits(3, List.of(first, upcoming, TEN_TO_ELEVEN), 0);

    assertThat(credits.pastAppointments(END)).containsExactly(TEN_TO_ELEVEN, first);
    assertThat(credits.pastAppointments(END.minusSeconds(1))).containsExactly(first);
  }

  @Test
  void anAppointmentIsUpcomingOrPastNeverBoth() {
    CaseCredits credits = credits(3, List.of(TEN_TO_ELEVEN), 0);

    for (Instant asOf : List.of(START.minusSeconds(1), START, END.minusSeconds(1), END)) {
      assertThat(credits.upcomingAppointments(asOf).size() + credits.pastAppointments(asOf).size())
          .as("at " + asOf)
          .isOne();
    }
  }

  @Test
  void theNextAppointmentIsTheSoonestUpcomingOne() {
    Appointment past = appointment("2026-11-16T09:00:00Z", "2026-11-16T10:00:00Z");
    Appointment later = appointment("2026-11-19T09:00:00Z", "2026-11-19T10:00:00Z");
    CaseCredits credits = credits(3, List.of(later, past, TEN_TO_ELEVEN), 0);

    assertThat(credits.nextAppointment(START.minusSeconds(1))).contains(TEN_TO_ELEVEN);
    assertThat(credits.nextAppointment(END.minusSeconds(1))).contains(TEN_TO_ELEVEN);
    assertThat(credits.nextAppointment(END)).contains(later);
    assertThat(credits.nextAppointment(later.end())).isEmpty();
  }

  /** A cancelled appointment is neither upcoming nor past: only the booked ones are. */
  @Test
  void aCancelledAppointmentIsNeitherUpcomingNorPast() {
    CaseCredits credits =
        new CaseCredits(new SessionCount(3), List.of(), List.of(cancelled(true), cancelled(false)));

    for (Instant asOf : List.of(START.minusSeconds(1), END)) {
      assertThat(credits.upcomingAppointments(asOf)).isEmpty();
      assertThat(credits.pastAppointments(asOf)).isEmpty();
      assertThat(credits.nextAppointment(asOf)).isEmpty();
    }
  }
}
