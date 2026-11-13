package com.cleanlearning.domain.consultation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.NonRefundableReason;
import com.cleanlearning.domain.offering.value.CancellationDeadline;

/**
 * UC-08's refund rule. Instants are in UTC; Zurich is UTC+1 in November and UTC+2 in summer time.
 */
class RefundEligibilityTest {

    private static final ConsultationCaseId CASE = new ConsultationCaseId(UUID.randomUUID());
    private static final CancellationDeadline DAY = new CancellationDeadline(24);

    /** Mon 16 Nov 2026, 10:00 in Zurich. */
    private static final Instant NOW = Instant.parse("2026-11-16T09:00:00Z");

    private static Appointment at(String start) {
        Instant s = Instant.parse(start);
        return new Appointment(new AppointmentId(UUID.randomUUID()), CASE, s, s.plusSeconds(3600));
    }

    private static RefundEligibility rule(List<Appointment> booked, int forfeited, CancellationDeadline deadline,
            Instant asOf) {
        return RefundEligibility.at(booked, forfeited, deadline, asOf);
    }

    @Test
    void aCaseWithoutAppointmentsAndForfeitedCreditsIsRefundEligible() {
        RefundEligibility result = rule(List.of(), 0, DAY, NOW);

        assertThat(result.isRefundEligible()).isTrue();
        assertThat(result.reason()).isEmpty();
        assertThat(result.deadlinePassedAppointment()).isEmpty();
        assertThat(result.freeCancellationUntil()).isEmpty();
    }

    @Test
    void appointmentsBeforeTheirDeadlineAreRefundEligible() {
        assertThat(rule(List.of(at("2026-11-20T09:00:00Z"), at("2026-11-25T09:00:00Z")), 0, DAY, NOW)
                .isRefundEligible()).isTrue();
    }

    @Test
    void aConsumedAppointmentIsASessionThatTookPlace() {
        RefundEligibility result = rule(List.of(at("2026-11-10T09:00:00Z"), at("2026-11-25T09:00:00Z")), 0, DAY, NOW);

        assertThat(result.isRefundEligible()).isFalse();
        assertThat(result.reason()).contains(NonRefundableReason.SESSION_TOOK_PLACE);
        assertThat(result.deadlinePassedAppointment()).isEmpty();
        assertThat(result.freeCancellationUntil()).isEmpty();
    }

    @Test
    void aRunningAppointmentIsASessionThatTookPlace() {
        // Started a minute ago, ends in 59 minutes.
        RefundEligibility result = rule(List.of(at("2026-11-16T08:59:00Z")), 0, DAY, NOW);

        assertThat(result.reason()).contains(NonRefundableReason.SESSION_TOOK_PLACE);
    }

    @Test
    void anAppointmentStartingNowIsASessionThatTookPlace() {
        assertThat(rule(List.of(at("2026-11-16T09:00:00Z")), 0, DAY, NOW).reason())
                .contains(NonRefundableReason.SESSION_TOOK_PLACE);
    }

    @Test
    void aForfeitedCreditIsReportedAlone() {
        RefundEligibility result = rule(List.of(), 1, DAY, NOW);

        assertThat(result.isRefundEligible()).isFalse();
        assertThat(result.reason()).contains(NonRefundableReason.CREDIT_FORFEITED);
        assertThat(result.deadlinePassedAppointment()).isEmpty();
        assertThat(result.freeCancellationUntil()).isEmpty();
    }

    @Test
    void severalForfeitedCreditsAreTheSameReason() {
        assertThat(rule(List.of(), 3, DAY, NOW).reason()).contains(NonRefundableReason.CREDIT_FORFEITED);
    }

    @Test
    void anAppointmentPastItsDeadlineButNotStartedNamesItAndItsFreeCancellationTime() {
        // Starts Tue 17 Nov 08:00 UTC; free until Mon 16 Nov 08:00 UTC, an hour before NOW.
        Appointment late = at("2026-11-17T08:00:00Z");
        RefundEligibility result = rule(List.of(late), 0, DAY, NOW);

        assertThat(result.isRefundEligible()).isFalse();
        assertThat(result.reason()).contains(NonRefundableReason.DEADLINE_PASSED);
        assertThat(result.deadlinePassedAppointment()).contains(late);
        assertThat(result.freeCancellationUntil()).contains(Instant.parse("2026-11-16T08:00:00Z"));
    }

    @Test
    void exactlyAtTheDeadlineTheAppointmentCountsAsPassed() {
        Appointment appointment = at("2026-11-17T09:00:00Z");

        RefundEligibility result = rule(List.of(appointment), 0, DAY, NOW);

        assertThat(result.reason()).contains(NonRefundableReason.DEADLINE_PASSED);
        assertThat(result.freeCancellationUntil()).contains(NOW);
    }

    @Test
    void aSecondBeforeTheDeadlineTheAppointmentIsStillFree() {
        Appointment appointment = at("2026-11-17T09:00:01Z");

        assertThat(rule(List.of(appointment), 0, DAY, NOW).isRefundEligible()).isTrue();
    }

    @Test
    void withoutADeadlineAnAppointmentIsFreeUntilItStarts() {
        CancellationDeadline none = new CancellationDeadline(0);

        assertThat(rule(List.of(at("2026-11-16T09:00:01Z")), 0, none, NOW).isRefundEligible()).isTrue();
        assertThat(rule(List.of(at("2026-11-16T09:00:00Z")), 0, none, NOW).reason())
                .contains(NonRefundableReason.SESSION_TOOK_PLACE);
    }

    @Test
    void deadlinePassedNamesTheSoonestLateAppointmentNotTheSoonestAppointment() {
        // The soonest appointment (in 14 days) is free; the next two (in 11 and 23 hours) are late.
        Appointment soonestLate = at("2026-11-16T20:00:00Z");
        Appointment laterLate = at("2026-11-17T08:00:00Z");
        Appointment free = at("2026-11-30T09:00:00Z");
        // Order of the list doesn't matter.
        RefundEligibility result = rule(List.of(free, laterLate, soonestLate), 0, DAY, NOW);

        assertThat(result.deadlinePassedAppointment()).contains(soonestLate);
        assertThat(result.freeCancellationUntil()).contains(Instant.parse("2026-11-15T20:00:00Z"));
    }

    @Test
    void aSoonerFreeAppointmentIsNotNamedWhenALaterOneIsLate() {
        // With a 7-day deadline, the appointment in 3 days is late and the one in 10 days isn't; with the list
        // reversed the answer is the same.
        CancellationDeadline week = new CancellationDeadline(168);
        Appointment late = at("2026-11-19T09:00:00Z");
        Appointment free = at("2026-11-26T09:00:00Z");

        assertThat(rule(List.of(free, late), 0, week, NOW).deadlinePassedAppointment()).contains(late);
        assertThat(rule(List.of(late, free), 0, week, NOW).deadlinePassedAppointment()).contains(late);
    }

    @Test
    void aSessionThatTookPlaceComesBeforeAForfeitedCreditAndAPassedDeadline() {
        RefundEligibility result = rule(List.of(at("2026-11-10T09:00:00Z"), at("2026-11-17T08:00:00Z")), 1, DAY, NOW);

        assertThat(result.reason()).contains(NonRefundableReason.SESSION_TOOK_PLACE);
        assertThat(result.deadlinePassedAppointment()).isEmpty();
    }

    @Test
    void aForfeitedCreditComesBeforeAPassedDeadline() {
        RefundEligibility result = rule(List.of(at("2026-11-17T08:00:00Z")), 1, DAY, NOW);

        assertThat(result.reason()).contains(NonRefundableReason.CREDIT_FORFEITED);
        assertThat(result.deadlinePassedAppointment()).isEmpty();
        assertThat(result.freeCancellationUntil()).isEmpty();
    }

    @Test
    void aSessionThatTookPlaceComesBeforeAForfeitedCredit() {
        assertThat(rule(List.of(at("2026-11-10T09:00:00Z")), 2, DAY, NOW).reason())
                .contains(NonRefundableReason.SESSION_TOOK_PLACE);
    }

    @Test
    void aRefundedCreditDoesNotCount() {
        // Appointments cancelled in time aren't booked and aren't forfeited: nothing is left to count.
        assertThat(rule(List.of(), 0, DAY, NOW).isRefundEligible()).isTrue();
    }

    @Test
    void countsRealHoursAcrossTheChangeToSummerTime() {
        // Sun 28 Mar 2027, 12:00 in Zurich (UTC+2) = 10:00 UTC; 24 real hours before is Sat 27 Mar 10:00 UTC.
        Appointment appointment = at("2027-03-28T10:00:00Z");

        assertThat(rule(List.of(appointment), 0, DAY, Instant.parse("2027-03-27T09:59:59Z")).isRefundEligible())
                .isTrue();
        RefundEligibility late = rule(List.of(appointment), 0, DAY, Instant.parse("2027-03-27T10:00:00Z"));
        assertThat(late.reason()).contains(NonRefundableReason.DEADLINE_PASSED);
        assertThat(late.freeCancellationUntil()).contains(Instant.parse("2027-03-27T10:00:00Z"));
    }

    @Test
    void theConstructorRefusesAnAppointmentWithoutDeadlinePassed() {
        Appointment appointment = at("2026-11-17T08:00:00Z");
        Optional<Instant> until = Optional.of(Instant.parse("2026-11-16T08:00:00Z"));

        assertThatIllegalArgumentException().isThrownBy(() -> new RefundEligibility(
                Optional.of(NonRefundableReason.CREDIT_FORFEITED), Optional.of(appointment), until));
        assertThatIllegalArgumentException().isThrownBy(
                () -> new RefundEligibility(Optional.empty(), Optional.of(appointment), until));
    }

    @Test
    void theConstructorRefusesDeadlinePassedWithoutTheAppointmentOrItsTime() {
        Appointment appointment = at("2026-11-17T08:00:00Z");
        Optional<NonRefundableReason> reason = Optional.of(NonRefundableReason.DEADLINE_PASSED);
        Optional<Instant> until = Optional.of(Instant.parse("2026-11-16T08:00:00Z"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RefundEligibility(reason, Optional.empty(), Optional.empty()));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RefundEligibility(reason, Optional.of(appointment), Optional.empty()));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RefundEligibility(reason, Optional.empty(), until));
    }
}
