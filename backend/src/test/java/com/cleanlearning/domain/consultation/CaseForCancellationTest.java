package com.cleanlearning.domain.consultation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.CancellationOutcome;
import com.cleanlearning.domain.consultation.value.CaseStatus;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.NonRefundableReason;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.BusinessTimeZone;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.shared.value.Money;

/**
 * UC-07's rules for cancelling an appointment and UC-08's for cancelling the case, as the plan's tables in
 * _docs/tasks/05-cancel-consultation-appointment-or-case/README.md list them: the appointment must be the case's, not
 * cancelled yet, not started; the case must be active and not completed; and the outcome the customer confirmed must be
 * the one at {@code now}. Career Coaching's terms; the appointment is on Tue 17 Nov 2026, 10:00–11:00, free to cancel
 * until Mon 16 Nov, 10:00, all in Zurich.
 */
class CaseForCancellationTest {

    private static final ConsultationCaseId CASE = new ConsultationCaseId(UUID.randomUUID());

    private static final PurchaseTerms THREE_SESSIONS = new PurchaseTerms(Money.chf("450.00"), new SessionCount(3),
            new SessionDuration(60), new CancellationDeadline(24));

    private static final Appointment TUESDAY = new Appointment(new AppointmentId(UUID.randomUUID()), CASE,
            zurich("2026-11-17T10:00"), zurich("2026-11-17T11:00"));

    /** Wed 18 Nov 2026, 10:00–11:00, free to cancel until Tue 17 Nov, 10:00. */
    private static final Appointment WEDNESDAY = new Appointment(new AppointmentId(UUID.randomUUID()), CASE,
            zurich("2026-11-18T10:00"), zurich("2026-11-18T11:00"));

    private static final PurchaseTerms ONE_SESSION = new PurchaseTerms(Money.chf("150.00"), new SessionCount(1),
            new SessionDuration(60), new CancellationDeadline(24));

    /** Sun 15 Nov 2026, 12:00: "now" in spec 05. */
    private static final Instant SUNDAY_NOON = zurich("2026-11-15T12:00");

    private static final AppointmentCancellation CANCELLED_IN_TIME = new AppointmentCancellation(
            new AppointmentId(UUID.randomUUID()), true);

    private static final AppointmentCancellation CANCELLED_LATE = new AppointmentCancellation(
            new AppointmentId(UUID.randomUUID()), false);

    private static final String APPOINTMENT_CANCELLED = "This appointment was already cancelled.";
    private static final String APPOINTMENT_STARTED = "This appointment can no longer be cancelled.";
    private static final String DEADLINE_PASSED =
            "The free cancellation deadline has just passed. Please confirm again.";
    private static final String CASE_CANCELLED = "This case was cancelled.";
    private static final String CASE_COMPLETED = "This case is completed.";
    private static final String REFUND_ELIGIBILITY_CHANGED =
            "Whether the purchase price would be refunded has changed. Please confirm again.";

    private static Instant zurich(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(BusinessTimeZone.ZONE).toInstant();
    }

    private static CaseForCancellation withTuesday() {
        return new CaseForCancellation(CASE, CaseStatus.ACTIVE, THREE_SESSIONS, List.of(TUESDAY),
                List.of(CANCELLED_IN_TIME, CANCELLED_LATE));
    }

    private static CaseForCancellation active(PurchaseTerms terms, List<Appointment> booked,
            List<AppointmentCancellation> cancelled) {
        return new CaseForCancellation(CASE, CaseStatus.ACTIVE, terms, booked, cancelled);
    }

    private static CaseCancellation cancellation(Instant now, boolean refundEligible,
            AppointmentCancellation... appointments) {
        return new CaseCancellation(CASE, new CancellationOutcome(now, refundEligible), List.of(appointments));
    }

    private static void assertRefused(Runnable cancellation, String code, String message) {
        BusinessRuleException refused = catchThrowableOfType(BusinessRuleException.class, cancellation::run);
        assertThat(refused).as("refused").isNotNull();
        assertThat(refused.code()).isEqualTo(code);
        assertThat(refused).hasMessage(message);
    }

    @Test
    void inTimeTheCreditIsRefunded() {
        assertThat(withTuesday().cancelAppointment(TUESDAY.id(), true, zurich("2026-11-15T12:00")))
                .isEqualTo(new AppointmentCancellation(TUESDAY.id(), true));
        assertThat(withTuesday().cancelAppointment(TUESDAY.id(), true, zurich("2026-11-16T09:59")))
                .isEqualTo(new AppointmentCancellation(TUESDAY.id(), true));
    }

    @Test
    void lateTheCreditIsForfeitedAlsoAtTheDeadline() {
        assertThat(withTuesday().cancelAppointment(TUESDAY.id(), false, zurich("2026-11-16T10:00")))
                .isEqualTo(new AppointmentCancellation(TUESDAY.id(), false));
        assertThat(withTuesday().cancelAppointment(TUESDAY.id(), false, zurich("2026-11-16T11:00")))
                .isEqualTo(new AppointmentCancellation(TUESDAY.id(), false));
        assertThat(withTuesday().cancelAppointment(TUESDAY.id(), false, zurich("2026-11-17T09:59")))
                .isEqualTo(new AppointmentCancellation(TUESDAY.id(), false));
    }

    @Test
    void anAppointmentThatIsNoneOfTheCasesIsNotFound() {
        AppointmentId another = new AppointmentId(UUID.randomUUID());

        assertThatThrownBy(() -> withTuesday().cancelAppointment(another, true, zurich("2026-11-15T12:00")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Appointment " + another.value() + " not found.");
    }

    @Test
    void anAppointmentCancelledBeforeIsAlreadyCancelledWhateverTheTime() {
        for (AppointmentCancellation cancelled : List.of(CANCELLED_IN_TIME, CANCELLED_LATE)) {
            assertRefused(() -> withTuesday().cancelAppointment(cancelled.appointmentId(), true,
                    zurich("2026-11-15T12:00")), CaseForCancellation.APPOINTMENT_CANCELLED, APPOINTMENT_CANCELLED);
            // before the started and the deadline checks
            assertRefused(() -> withTuesday().cancelAppointment(cancelled.appointmentId(), true,
                    zurich("2026-11-18T12:00")), CaseForCancellation.APPOINTMENT_CANCELLED, APPOINTMENT_CANCELLED);
        }
    }

    @Test
    void fromItsStartTheAppointmentCanNoLongerBeCancelledWhateverWasConfirmed() {
        for (boolean confirmed : List.of(true, false)) {
            assertRefused(() -> withTuesday().cancelAppointment(TUESDAY.id(), confirmed, zurich("2026-11-17T10:00")),
                    CaseForCancellation.APPOINTMENT_STARTED, APPOINTMENT_STARTED);
            assertRefused(() -> withTuesday().cancelAppointment(TUESDAY.id(), confirmed, zurich("2026-11-17T10:30")),
                    CaseForCancellation.APPOINTMENT_STARTED, APPOINTMENT_STARTED);
            assertRefused(() -> withTuesday().cancelAppointment(TUESDAY.id(), confirmed, zurich("2026-11-18T12:00")),
                    CaseForCancellation.APPOINTMENT_STARTED, APPOINTMENT_STARTED);
        }
    }

    /** Spec 05, decision 4: the dialog said the credit would be returned, but the deadline passed meanwhile. */
    @Test
    void aRefundConfirmedAfterTheDeadlineIsRefused() {
        assertRefused(() -> withTuesday().cancelAppointment(TUESDAY.id(), true, zurich("2026-11-16T10:01")),
                CaseForCancellation.DEADLINE_PASSED, DEADLINE_PASSED);
        assertRefused(() -> withTuesday().cancelAppointment(TUESDAY.id(), true, zurich("2026-11-16T10:00")),
                CaseForCancellation.DEADLINE_PASSED, DEADLINE_PASSED);
    }

    /** Spec 05, decision 4: the server never does what the customer didn't confirm, not even in their favour. */
    @Test
    void aLateCancellationConfirmedInTimeIsRefused() {
        assertRefused(() -> withTuesday().cancelAppointment(TUESDAY.id(), false, zurich("2026-11-15T12:00")),
                CaseForCancellation.DEADLINE_PASSED, DEADLINE_PASSED);
    }

    @Test
    void theForfeitedCreditsAreTheAppointmentsCancelledLate() {
        assertThat(withTuesday().forfeitedCredits()).isOne();
        assertThat(new CaseForCancellation(CASE, CaseStatus.ACTIVE, THREE_SESSIONS, List.of(), List.of())
                .forfeitedCredits()).isZero();
        assertThat(new CaseForCancellation(CASE, CaseStatus.ACTIVE, THREE_SESSIONS, List.of(),
                List.of(CANCELLED_LATE, new AppointmentCancellation(new AppointmentId(UUID.randomUUID()), false)))
                .forfeitedCredits()).isEqualTo(2);
    }

    @Test
    void anAppointmentIsEitherBookedOrCancelledNeverBoth() {
        assertThatIllegalArgumentException().isThrownBy(() -> new CaseForCancellation(CASE, CaseStatus.ACTIVE,
                THREE_SESSIONS, List.of(TUESDAY), List.of(new AppointmentCancellation(TUESDAY.id(), true))));
    }

    @Test
    void aCaseWithoutAppointmentsIsCancelledRefundEligible() {
        assertThat(active(THREE_SESSIONS, List.of(), List.of()).cancel(true, SUNDAY_NOON))
                .isEqualTo(cancellation(SUNDAY_NOON, true));
    }

    /** Spec 05: the future appointment is cancelled with the case, its credit refunded as UC-07 would refund it. */
    @Test
    void inTimeTheCaseIsRefundEligibleAndItsAppointmentsCreditIsRefunded() {
        assertThat(active(THREE_SESSIONS, List.of(TUESDAY, WEDNESDAY), List.of()).cancel(true, SUNDAY_NOON))
                .isEqualTo(cancellation(SUNDAY_NOON, true, new AppointmentCancellation(TUESDAY.id(), true),
                        new AppointmentCancellation(WEDNESDAY.id(), true)));
    }

    /** Spec 05, decision 7: each appointment follows UC-07's rule at the moment: forfeited late, refunded in time. */
    @Test
    void eachAppointmentCancelledWithTheCaseHasItsCreditAsItsOwnDeadlineDecides() {
        Instant mondayEleven = zurich("2026-11-16T11:00");

        assertThat(active(THREE_SESSIONS, List.of(WEDNESDAY, TUESDAY), List.of()).cancel(false, mondayEleven))
                .isEqualTo(cancellation(mondayEleven, false, new AppointmentCancellation(WEDNESDAY.id(), true),
                        new AppointmentCancellation(TUESDAY.id(), false)));
    }

    /** A running appointment is consumed: it isn't cancelled, and the case isn't refund-eligible. */
    @Test
    void aRunningOrPastAppointmentIsNotCancelled() {
        Appointment monday = new Appointment(new AppointmentId(UUID.randomUUID()), CASE, zurich("2026-11-16T10:00"),
                zurich("2026-11-16T11:00"));
        Instant tuesdayHalfPastTen = zurich("2026-11-17T10:30");

        assertThat(active(THREE_SESSIONS, List.of(monday, TUESDAY, WEDNESDAY), List.of())
                .cancel(false, tuesdayHalfPastTen))
                .isEqualTo(cancellation(tuesdayHalfPastTen, false, new AppointmentCancellation(WEDNESDAY.id(), false)));
        // while the last session of a fully booked case runs, it isn't completed yet: nothing is left to cancel
        assertThat(active(ONE_SESSION, List.of(TUESDAY), List.of()).cancel(false, tuesdayHalfPastTen))
                .isEqualTo(cancellation(tuesdayHalfPastTen, false));
    }

    @Test
    void theRefundEligibilityIsUc08sRuleWithTheCasesForfeitedCredits() {
        assertThat(withTuesday().refundEligibility(SUNDAY_NOON).reason())
                .contains(NonRefundableReason.CREDIT_FORFEITED);
        assertThat(active(THREE_SESSIONS, List.of(TUESDAY), List.of(CANCELLED_IN_TIME)).refundEligibility(SUNDAY_NOON)
                .isRefundEligible()).isTrue();
        assertThat(active(THREE_SESSIONS, List.of(TUESDAY), List.of()).refundEligibility(zurich("2026-11-16T10:00"))
                .reason()).contains(NonRefundableReason.DEADLINE_PASSED);
        // a case with a forfeited credit cancels not refund-eligible
        assertThat(withTuesday().cancel(false, SUNDAY_NOON)).isEqualTo(cancellation(SUNDAY_NOON, false,
                new AppointmentCancellation(TUESDAY.id(), true)));
    }

    @Test
    void aCancelledCaseCantBeCancelledAgainWhateverElse() {
        for (boolean confirmed : List.of(true, false)) {
            assertRefused(() -> new CaseForCancellation(CASE, CaseStatus.CANCELLED, THREE_SESSIONS, List.of(),
                    List.of(CANCELLED_IN_TIME)).cancel(confirmed, SUNDAY_NOON), CaseForCancellation.CASE_CANCELLED,
                    CASE_CANCELLED);
            // before the completed and the outcome checks
            assertRefused(() -> new CaseForCancellation(CASE, CaseStatus.CANCELLED, ONE_SESSION, List.of(TUESDAY),
                    List.of()).cancel(confirmed, zurich("2026-11-18T12:00")), CaseForCancellation.CASE_CANCELLED,
                    CASE_CANCELLED);
        }
    }

    /** Spec 05, decision 13: completed once no credit is open and the last booked appointment has ended. */
    @Test
    void aCompletedCaseCantBeCancelledWhateverWasConfirmed() {
        for (boolean confirmed : List.of(true, false)) {
            assertRefused(() -> active(ONE_SESSION, List.of(TUESDAY), List.of()).cancel(confirmed,
                    TUESDAY.end()), CaseForCancellation.CASE_COMPLETED, CASE_COMPLETED);
            // every credit forfeited
            assertRefused(() -> active(ONE_SESSION, List.of(), List.of(CANCELLED_LATE)).cancel(confirmed,
                    SUNDAY_NOON), CaseForCancellation.CASE_COMPLETED, CASE_COMPLETED);
        }
        // an open credit left: not completed
        assertThat(active(THREE_SESSIONS, List.of(TUESDAY), List.of()).cancel(false, TUESDAY.end()))
                .isEqualTo(cancellation(TUESDAY.end(), false));
    }

    /** Spec 05, decision 4: the dialog said the price would be refunded, but a deadline passed meanwhile. */
    @Test
    void aRefundConfirmedAfterADeadlinePassedIsRefused() {
        for (String now : List.of("2026-11-16T10:01", "2026-11-16T10:00")) {
            assertRefused(() -> active(THREE_SESSIONS, List.of(TUESDAY), List.of()).cancel(true, zurich(now)),
                    CaseForCancellation.REFUND_ELIGIBILITY_CHANGED, REFUND_ELIGIBILITY_CHANGED);
        }
    }

    /** Spec 05, decision 4: the server never does what the customer didn't confirm, not even in their favour. */
    @Test
    void aCancellationWithoutRefundConfirmedForARefundEligibleCaseIsRefused() {
        assertRefused(() -> active(THREE_SESSIONS, List.of(TUESDAY), List.of()).cancel(false, SUNDAY_NOON),
                CaseForCancellation.REFUND_ELIGIBILITY_CHANGED, REFUND_ELIGIBILITY_CHANGED);
        assertRefused(() -> active(THREE_SESSIONS, List.of(), List.of()).cancel(false, SUNDAY_NOON),
                CaseForCancellation.REFUND_ELIGIBILITY_CHANGED, REFUND_ELIGIBILITY_CHANGED);
    }
}
