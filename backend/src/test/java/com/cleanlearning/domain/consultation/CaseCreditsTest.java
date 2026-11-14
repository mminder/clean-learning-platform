package com.cleanlearning.domain.consultation;

import static org.assertj.core.api.Assertions.assertThat;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.offering.value.SessionCount;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * The credits a case's booked appointments and its forfeited ones use, and when that completes the
 * case (glossary, spec 05 decision 13). Instants are in UTC.
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

  @Test
  void theOpenCreditsAreTheSessionsLessTheBookedAppointmentsConsumedOrNot() {
    assertThat(CaseCredits.openCredits(new SessionCount(3), 0, 0)).isEqualTo(3);
    assertThat(CaseCredits.openCredits(new SessionCount(3), 2, 0)).isEqualTo(1);
    assertThat(CaseCredits.openCredits(new SessionCount(3), 3, 0)).isZero();
    assertThat(CaseCredits.openCredits(new SessionCount(5), 1, 0)).isEqualTo(4);
  }

  @Test
  void theOpenCreditsAlsoLackTheForfeitedOnes() {
    assertThat(CaseCredits.openCredits(new SessionCount(3), 0, 1)).isEqualTo(2);
    // one consumed, one cancelled in time (neither booked nor forfeited), one cancelled late
    assertThat(CaseCredits.openCredits(new SessionCount(3), 1, 1)).isOne();
    assertThat(CaseCredits.openCredits(new SessionCount(3), 2, 1)).isZero();
    assertThat(CaseCredits.openCredits(new SessionCount(1), 0, 1)).isZero();
  }

  /**
   * Spec 05, decision 13: completed once no credit is open and the last booked appointment has
   * ended.
   */
  @Test
  void aCaseIsCompletedWithoutOpenCreditsOnceItsLastAppointmentHasEnded() {
    Appointment earlier = appointment("2026-11-16T09:00:00Z", "2026-11-16T10:00:00Z");
    List<Appointment> both = List.of(TEN_TO_ELEVEN, earlier);
    SessionCount two = new SessionCount(2);

    assertThat(CaseCredits.isCompleted(two, both, 0, START.minusSeconds(1))).isFalse();
    // while the last session runs, it isn't completed yet
    assertThat(CaseCredits.isCompleted(two, both, 0, START)).isFalse();
    assertThat(CaseCredits.isCompleted(two, both, 0, END.minusSeconds(1))).isFalse();
    assertThat(CaseCredits.isCompleted(two, both, 0, END)).isTrue();
  }

  @Test
  void aCaseWithAnOpenCreditIsNotCompleted() {
    assertThat(CaseCredits.isCompleted(new SessionCount(3), List.of(TEN_TO_ELEVEN), 0, END))
        .isFalse();
    assertThat(CaseCredits.isCompleted(new SessionCount(3), List.of(), 0, END)).isFalse();
    // one consumed, one forfeited, one open
    assertThat(CaseCredits.isCompleted(new SessionCount(3), List.of(TEN_TO_ELEVEN), 1, END))
        .isFalse();
  }

  @Test
  void forfeitedCreditsCanCompleteACase() {
    assertThat(CaseCredits.isCompleted(new SessionCount(1), List.of(), 1, START)).isTrue();
    assertThat(CaseCredits.isCompleted(new SessionCount(2), List.of(TEN_TO_ELEVEN), 1, END))
        .isTrue();
    assertThat(CaseCredits.isCompleted(new SessionCount(2), List.of(TEN_TO_ELEVEN), 1, START))
        .isFalse();
  }
}
