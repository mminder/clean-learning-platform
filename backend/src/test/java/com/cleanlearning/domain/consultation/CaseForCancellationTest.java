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
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.BusinessTimeZone;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.shared.value.Money;

/**
 * UC-07's rules for cancelling an appointment, as the plan's table in
 * _docs/tasks/05-cancel-consultation-appointment-or-case/README.md lists them: the appointment must be the case's, not
 * cancelled yet, not started, and the outcome the customer confirmed must be the one at {@code now}. Career Coaching's
 * terms; the appointment is on Tue 17 Nov 2026, 10:00–11:00, free to cancel until Mon 16 Nov, 10:00, all in Zurich.
 */
class CaseForCancellationTest {

    private static final ConsultationCaseId CASE = new ConsultationCaseId(UUID.randomUUID());

    private static final PurchaseTerms THREE_SESSIONS = new PurchaseTerms(Money.chf("450.00"), new SessionCount(3),
            new SessionDuration(60), new CancellationDeadline(24));

    private static final Appointment TUESDAY = new Appointment(new AppointmentId(UUID.randomUUID()), CASE,
            zurich("2026-11-17T10:00"), zurich("2026-11-17T11:00"));

    private static final AppointmentCancellation CANCELLED_IN_TIME = new AppointmentCancellation(
            new AppointmentId(UUID.randomUUID()), true);

    private static final AppointmentCancellation CANCELLED_LATE = new AppointmentCancellation(
            new AppointmentId(UUID.randomUUID()), false);

    private static final String APPOINTMENT_CANCELLED = "This appointment was already cancelled.";
    private static final String APPOINTMENT_STARTED = "This appointment can no longer be cancelled.";
    private static final String DEADLINE_PASSED =
            "The free cancellation deadline has just passed. Please confirm again.";

    private static Instant zurich(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(BusinessTimeZone.ZONE).toInstant();
    }

    private static CaseForCancellation withTuesday() {
        return new CaseForCancellation(CASE, THREE_SESSIONS, List.of(TUESDAY),
                List.of(CANCELLED_IN_TIME, CANCELLED_LATE));
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
        assertThat(new CaseForCancellation(CASE, THREE_SESSIONS, List.of(), List.of()).forfeitedCredits()).isZero();
        assertThat(new CaseForCancellation(CASE, THREE_SESSIONS, List.of(),
                List.of(CANCELLED_LATE, new AppointmentCancellation(new AppointmentId(UUID.randomUUID()), false)))
                .forfeitedCredits()).isEqualTo(2);
    }

    @Test
    void anAppointmentIsEitherBookedOrCancelledNeverBoth() {
        assertThatIllegalArgumentException().isThrownBy(() -> new CaseForCancellation(CASE, THREE_SESSIONS,
                List.of(TUESDAY), List.of(new AppointmentCancellation(TUESDAY.id(), true))));
    }
}
