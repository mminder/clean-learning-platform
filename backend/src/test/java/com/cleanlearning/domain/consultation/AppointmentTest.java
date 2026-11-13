package com.cleanlearning.domain.consultation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.shared.BusinessTimeZone;

/**
 * A booked appointment at a given time (glossary, spec 04 decision 21): {@code BOOKED} until its start, then
 * {@code CONSUMED}; upcoming until its end, past from its end on; the credits the booked appointments and the forfeited
 * ones use; and UC-07's cancellation rules: cancellable until its start, free until the deadline before it.
 * Instants are in UTC; Zurich is UTC+1 in November.
 */
class AppointmentTest {

    private static final ConsultationCaseId CASE = new ConsultationCaseId(UUID.randomUUID());

    /** Tue 17 Nov 2026, 10:00–11:00 in Zurich. */
    private static final Instant START = Instant.parse("2026-11-17T09:00:00Z");
    private static final Instant END = Instant.parse("2026-11-17T10:00:00Z");

    private static final Appointment TEN_TO_ELEVEN = new Appointment(new AppointmentId(UUID.randomUUID()), CASE, START,
            END);

    private static Appointment appointment(String start, String end) {
        return new Appointment(new AppointmentId(UUID.randomUUID()), CASE, Instant.parse(start), Instant.parse(end));
    }

    private static Instant zurich(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(BusinessTimeZone.ZONE).toInstant();
    }

    @Test
    void isBookedUntilItsStartAndConsumedFromItsStartOn() {
        assertThat(TEN_TO_ELEVEN.statusAt(START.minusSeconds(1))).isEqualTo(AppointmentStatus.BOOKED);
        assertThat(TEN_TO_ELEVEN.statusAt(START)).isEqualTo(AppointmentStatus.CONSUMED);
        assertThat(TEN_TO_ELEVEN.statusAt(END)).isEqualTo(AppointmentStatus.CONSUMED);
        assertThat(TEN_TO_ELEVEN.statusAt(END.plusSeconds(86_400))).isEqualTo(AppointmentStatus.CONSUMED);
    }

    @Test
    void isUpcomingUntilItsEndAlsoWhileItRuns() {
        assertThat(TEN_TO_ELEVEN.isUpcomingAt(START.minusSeconds(1))).isTrue();
        assertThat(TEN_TO_ELEVEN.isUpcomingAt(START)).isTrue();
        assertThat(TEN_TO_ELEVEN.isUpcomingAt(END.minusSeconds(1))).isTrue();
        assertThat(TEN_TO_ELEVEN.isUpcomingAt(END)).isFalse();
    }

    @Test
    void asTheCustomerSeesItCarriesTheStatusAtThatTime() {
        assertThat(TEN_TO_ELEVEN.at(START.minusSeconds(1)))
                .isEqualTo(new CustomerAppointment(TEN_TO_ELEVEN.id(), START, END, AppointmentStatus.BOOKED));
        assertThat(TEN_TO_ELEVEN.at(START))
                .isEqualTo(new CustomerAppointment(TEN_TO_ELEVEN.id(), START, END, AppointmentStatus.CONSUMED));
    }

    @Test
    void theUpcomingOnesAreTheOnesNotEndedSoonestFirstARunningOneConsumed() {
        Appointment past = appointment("2026-11-16T09:00:00Z", "2026-11-16T10:00:00Z");
        Appointment running = appointment("2026-11-17T09:00:00Z", "2026-11-17T10:00:00Z");
        Appointment later = appointment("2026-11-20T09:00:00Z", "2026-11-20T10:00:00Z");
        Appointment soon = appointment("2026-11-18T09:00:00Z", "2026-11-18T10:00:00Z");
        Instant asOf = Instant.parse("2026-11-17T09:30:00Z");

        assertThat(Appointment.upcoming(List.of(later, past, soon, running), asOf)).containsExactly(
                running.at(asOf), soon.at(asOf), later.at(asOf));
        assertThat(Appointment.upcoming(List.of(later, past, soon, running), asOf))
                .extracting(CustomerAppointment::status)
                .containsExactly(AppointmentStatus.CONSUMED, AppointmentStatus.BOOKED, AppointmentStatus.BOOKED);
    }

    @Test
    void thePastOnesAreTheOnesEndedNewestFirstAllConsumed() {
        Appointment first = appointment("2026-11-16T09:00:00Z", "2026-11-16T10:00:00Z");
        Appointment second = appointment("2026-11-17T09:00:00Z", "2026-11-17T10:00:00Z");
        Appointment upcoming = appointment("2026-11-20T09:00:00Z", "2026-11-20T10:00:00Z");
        Instant asOf = Instant.parse("2026-11-17T10:00:00Z");

        assertThat(Appointment.past(List.of(first, upcoming, second), asOf)).containsExactly(second.at(asOf),
                first.at(asOf));
        assertThat(Appointment.past(List.of(first, upcoming, second), asOf)).extracting(CustomerAppointment::status)
                .containsOnly(AppointmentStatus.CONSUMED);
    }

    @Test
    void anAppointmentIsUpcomingOrPastNeverBoth() {
        Appointment appointment = TEN_TO_ELEVEN;
        for (Instant asOf : List.of(START.minusSeconds(1), START, END.minusSeconds(1), END)) {
            assertThat(Appointment.upcoming(List.of(appointment), asOf).size()
                    + Appointment.past(List.of(appointment), asOf).size()).as("at " + asOf).isOne();
        }
    }

    @Test
    void theOpenCreditsAreTheSessionsLessTheBookedAppointmentsConsumedOrNot() {
        assertThat(Appointment.openCredits(new SessionCount(3), 0, 0)).isEqualTo(3);
        assertThat(Appointment.openCredits(new SessionCount(3), 2, 0)).isEqualTo(1);
        assertThat(Appointment.openCredits(new SessionCount(3), 3, 0)).isZero();
        assertThat(Appointment.openCredits(new SessionCount(5), 1, 0)).isEqualTo(4);
    }

    @Test
    void theOpenCreditsAlsoLackTheForfeitedOnes() {
        assertThat(Appointment.openCredits(new SessionCount(3), 0, 1)).isEqualTo(2);
        // one consumed, one cancelled in time (neither booked nor forfeited), one cancelled late
        assertThat(Appointment.openCredits(new SessionCount(3), 1, 1)).isOne();
        assertThat(Appointment.openCredits(new SessionCount(3), 2, 1)).isZero();
        assertThat(Appointment.openCredits(new SessionCount(1), 0, 1)).isZero();
    }

    /** Spec 05, decision 13: completed once no credit is open and the last booked appointment has ended. */
    @Test
    void aCaseIsCompletedWithoutOpenCreditsOnceItsLastAppointmentHasEnded() {
        Appointment earlier = appointment("2026-11-16T09:00:00Z", "2026-11-16T10:00:00Z");
        List<Appointment> both = List.of(TEN_TO_ELEVEN, earlier);
        SessionCount two = new SessionCount(2);

        assertThat(Appointment.isCompleted(two, both, 0, START.minusSeconds(1))).isFalse();
        // while the last session runs, it isn't completed yet
        assertThat(Appointment.isCompleted(two, both, 0, START)).isFalse();
        assertThat(Appointment.isCompleted(two, both, 0, END.minusSeconds(1))).isFalse();
        assertThat(Appointment.isCompleted(two, both, 0, END)).isTrue();
    }

    @Test
    void aCaseWithAnOpenCreditIsNotCompleted() {
        assertThat(Appointment.isCompleted(new SessionCount(3), List.of(TEN_TO_ELEVEN), 0, END)).isFalse();
        assertThat(Appointment.isCompleted(new SessionCount(3), List.of(), 0, END)).isFalse();
        // one consumed, one forfeited, one open
        assertThat(Appointment.isCompleted(new SessionCount(3), List.of(TEN_TO_ELEVEN), 1, END)).isFalse();
    }

    @Test
    void forfeitedCreditsCanCompleteACase() {
        assertThat(Appointment.isCompleted(new SessionCount(1), List.of(), 1, START)).isTrue();
        assertThat(Appointment.isCompleted(new SessionCount(2), List.of(TEN_TO_ELEVEN), 1, END)).isTrue();
        assertThat(Appointment.isCompleted(new SessionCount(2), List.of(TEN_TO_ELEVEN), 1, START)).isFalse();
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

        assertThat(TEN_TO_ELEVEN.isCreditRefundedIfCancelledAt(hours24, Instant.parse("2026-11-15T11:00:00Z")))
                .isTrue();
        assertThat(TEN_TO_ELEVEN.isCreditRefundedIfCancelledAt(hours24, freeUntil.minusMillis(1))).isTrue();
        assertThat(TEN_TO_ELEVEN.isCreditRefundedIfCancelledAt(hours24, freeUntil)).isFalse();
        assertThat(TEN_TO_ELEVEN.isCreditRefundedIfCancelledAt(hours24, freeUntil.plusSeconds(60))).isFalse();
        assertThat(TEN_TO_ELEVEN.isCreditRefundedIfCancelledAt(hours24, START.minusSeconds(1))).isFalse();
    }

    @Test
    void withADeadlineOfZeroHoursTheCreditIsRefundedUntilTheStart() {
        CancellationDeadline none = new CancellationDeadline(0);

        assertThat(TEN_TO_ELEVEN.isCreditRefundedIfCancelledAt(none, START.minusSeconds(1))).isTrue();
        assertThat(TEN_TO_ELEVEN.isCreditRefundedIfCancelledAt(none, START)).isFalse();
    }

    /**
     * Spec 05: on Sun 28 Mar 2027 Zurich's clocks jump from 02:00 to 03:00, so 10:00 that day (08:00Z) is 24 real
     * hours after Sat 27 Mar, 09:00 (08:00Z), not 10:00. On Sun 31 Oct 2027 they go back from 03:00 to 02:00, so 10:00
     * that day (09:00Z) is 24 real hours after Sat 30 Oct, 11:00 (09:00Z).
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
    void theSortedUpcomingOnesAreTheAppointmentsThemselves() {
        Appointment past = appointment("2026-11-16T09:00:00Z", "2026-11-16T10:00:00Z");
        Appointment later = appointment("2026-11-20T09:00:00Z", "2026-11-20T10:00:00Z");
        Appointment soon = appointment("2026-11-18T09:00:00Z", "2026-11-18T10:00:00Z");

        assertThat(Appointment.sortedUpcoming(List.of(later, past, soon), Instant.parse("2026-11-17T09:30:00Z")))
                .containsExactly(soon, later);
    }

    @Test
    void endsAfterItStarts() {
        AppointmentId id = new AppointmentId(UUID.randomUUID());

        assertThatIllegalArgumentException().isThrownBy(() -> new Appointment(id, CASE, END, START))
                .withMessage("An appointment ends after it starts: " + END + " to " + START + ".");
        assertThatIllegalArgumentException().isThrownBy(() -> new Appointment(id, CASE, START, START));
    }

    @Test
    void needsAllItsFields() {
        AppointmentId id = new AppointmentId(UUID.randomUUID());

        assertThatNullPointerException().isThrownBy(() -> new Appointment(null, CASE, START, END)).withMessage("id");
        assertThatNullPointerException().isThrownBy(() -> new Appointment(id, null, START, END))
                .withMessage("consultationCaseId");
        assertThatNullPointerException().isThrownBy(() -> new Appointment(id, CASE, null, END)).withMessage("start");
        assertThatNullPointerException().isThrownBy(() -> new Appointment(id, CASE, START, null)).withMessage("end");
    }
}
