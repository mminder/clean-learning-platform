package com.cleanlearning.domain.consultation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.shared.BusinessTimeZone;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * A booked appointment at a given time (glossary, spec 04 decision 21): {@code BOOKED} until its
 * start, then {@code CONSUMED}; upcoming until its end, past from its end on; and UC-07's
 * cancellation rules: cancellable until its start, free until the deadline before it. Instants are
 * in UTC; Zurich is UTC+1 in November.
 */
class AppointmentTest {

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

  private static Instant zurich(String localDateTime) {
    return LocalDateTime.parse(localDateTime).atZone(BusinessTimeZone.ZONE).toInstant();
  }

  @Test
  void isBookedUntilItsStartAndConsumedFromItsStartOn() {
    assertThat(TEN_TO_ELEVEN.statusAt(START.minusSeconds(1))).isEqualTo(AppointmentStatus.BOOKED);
    assertThat(TEN_TO_ELEVEN.statusAt(START)).isEqualTo(AppointmentStatus.CONSUMED);
    assertThat(TEN_TO_ELEVEN.statusAt(END)).isEqualTo(AppointmentStatus.CONSUMED);
    assertThat(TEN_TO_ELEVEN.statusAt(END.plusSeconds(86_400)))
        .isEqualTo(AppointmentStatus.CONSUMED);
  }

  @Test
  void isUpcomingUntilItsEndAlsoWhileItRuns() {
    assertThat(TEN_TO_ELEVEN.isUpcomingAt(START.minusSeconds(1))).isTrue();
    assertThat(TEN_TO_ELEVEN.isUpcomingAt(START)).isTrue();
    assertThat(TEN_TO_ELEVEN.isUpcomingAt(END.minusSeconds(1))).isTrue();
    assertThat(TEN_TO_ELEVEN.isUpcomingAt(END)).isFalse();
  }

  @Test
  void cancellingIsFreeUntilTheDeadlineBeforeItsStartInRealHours() {
    assertThat(TEN_TO_ELEVEN.freeCancellationUntil(new CancellationDeadline(24)))
        .isEqualTo(Instant.parse("2026-11-16T09:00:00Z"));
    assertThat(TEN_TO_ELEVEN.freeCancellationUntil(new CancellationDeadline(72)))
        .isEqualTo(Instant.parse("2026-11-14T09:00:00Z"));
    assertThat(TEN_TO_ELEVEN.freeCancellationUntil(new CancellationDeadline(0))).isEqualTo(START);
  }

  @Test
  void theCreditIsRefundedOnlyBeforeTheDeadlineAtTheDeadlineItIsLate() {
    CancellationDeadline hours24 = new CancellationDeadline(24);
    Instant freeUntil = Instant.parse("2026-11-16T09:00:00Z");

    assertThat(
            TEN_TO_ELEVEN.isCreditRefundedIfCancelledAt(
                hours24, Instant.parse("2026-11-15T11:00:00Z")))
        .isTrue();
    assertThat(TEN_TO_ELEVEN.isCreditRefundedIfCancelledAt(hours24, freeUntil.minusMillis(1)))
        .isTrue();
    assertThat(TEN_TO_ELEVEN.isCreditRefundedIfCancelledAt(hours24, freeUntil)).isFalse();
    assertThat(TEN_TO_ELEVEN.isCreditRefundedIfCancelledAt(hours24, freeUntil.plusSeconds(60)))
        .isFalse();
    assertThat(TEN_TO_ELEVEN.isCreditRefundedIfCancelledAt(hours24, START.minusSeconds(1)))
        .isFalse();
  }

  @Test
  void withADeadlineOfZeroHoursTheCreditIsRefundedUntilTheStart() {
    CancellationDeadline none = new CancellationDeadline(0);

    assertThat(TEN_TO_ELEVEN.isCreditRefundedIfCancelledAt(none, START.minusSeconds(1))).isTrue();
    assertThat(TEN_TO_ELEVEN.isCreditRefundedIfCancelledAt(none, START)).isFalse();
  }

  /**
   * Spec 05: on Sun 28 Mar 2027 Zurich's clocks jump from 02:00 to 03:00, so 10:00 that day
   * (08:00Z) is 24 real hours after Sat 27 Mar, 09:00 (08:00Z), not 10:00. On Sun 31 Oct 2027 they
   * go back from 03:00 to 02:00, so 10:00 that day (09:00Z) is 24 real hours after Sat 30 Oct,
   * 11:00 (09:00Z).
   */
  @Test
  void onBothDaysOfTheSummerTimeChangesTheDeadlineIsTwentyFourRealHoursBeforeTheStart() {
    CancellationDeadline hours24 = new CancellationDeadline(24);
    Appointment spring = appointment("2027-03-28T08:00:00Z", "2027-03-28T09:00:00Z");
    Appointment autumn = appointment("2027-10-31T09:00:00Z", "2027-10-31T10:00:00Z");

    assertThat(spring.freeCancellationUntil(hours24)).isEqualTo(zurich("2027-03-27T09:00"));
    assertThat(spring.isCreditRefundedIfCancelledAt(hours24, zurich("2027-03-27T08:59"))).isTrue();
    assertThat(spring.isCreditRefundedIfCancelledAt(hours24, zurich("2027-03-27T09:00"))).isFalse();

    assertThat(autumn.freeCancellationUntil(hours24)).isEqualTo(zurich("2027-10-30T11:00"));
    assertThat(autumn.isCreditRefundedIfCancelledAt(hours24, zurich("2027-10-30T10:59"))).isTrue();
    assertThat(autumn.isCreditRefundedIfCancelledAt(hours24, zurich("2027-10-30T11:00"))).isFalse();
  }

  @Test
  void itCanBeCancelledUntilItStartsNotWhileItRuns() {
    assertThat(TEN_TO_ELEVEN.isCancellableAt(Instant.parse("2026-11-15T11:00:00Z"))).isTrue();
    assertThat(TEN_TO_ELEVEN.isCancellableAt(START.minusMillis(1))).isTrue();
    assertThat(TEN_TO_ELEVEN.isCancellableAt(START)).isFalse();
    assertThat(TEN_TO_ELEVEN.isCancellableAt(Instant.parse("2026-11-17T09:30:00Z"))).isFalse();
    assertThat(TEN_TO_ELEVEN.isCancellableAt(END)).isFalse();
  }

  @Test
  void endsAfterItStarts() {
    AppointmentId id = new AppointmentId(UUID.randomUUID());

    assertThatIllegalArgumentException()
        .isThrownBy(() -> new Appointment(id, CASE, END, START))
        .withMessage("An appointment ends after it starts: " + END + " to " + START + ".");
    assertThatIllegalArgumentException().isThrownBy(() -> new Appointment(id, CASE, START, START));
  }

  @Test
  void needsAllItsFields() {
    AppointmentId id = new AppointmentId(UUID.randomUUID());

    assertThatNullPointerException()
        .isThrownBy(() -> new Appointment(null, CASE, START, END))
        .withMessage("id");
    assertThatNullPointerException()
        .isThrownBy(() -> new Appointment(id, null, START, END))
        .withMessage("consultationCaseId");
    assertThatNullPointerException()
        .isThrownBy(() -> new Appointment(id, CASE, null, END))
        .withMessage("start");
    assertThatNullPointerException()
        .isThrownBy(() -> new Appointment(id, CASE, START, null))
        .withMessage("end");
  }
}
