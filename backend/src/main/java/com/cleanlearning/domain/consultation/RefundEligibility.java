package com.cleanlearning.domain.consultation;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.cleanlearning.domain.consultation.value.NonRefundableReason;
import com.cleanlearning.domain.offering.value.CancellationDeadline;

/**
 * Whether cancelling a case refunds its purchase price (UC-08, D-12, spec 05 decision 5): the one rule the case
 * detail's preview and the cancellation share. The reason is empty when the case is refund-eligible, otherwise the
 * first that applies, in the order of {@link NonRefundableReason}. With {@code DEADLINE_PASSED} only, it names the
 * soonest appointment whose free-cancellation time has passed and that time.
 */
public record RefundEligibility(Optional<NonRefundableReason> reason, Optional<Appointment> deadlinePassedAppointment,
        Optional<Instant> freeCancellationUntil) {

    public RefundEligibility {
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(deadlinePassedAppointment, "deadlinePassedAppointment");
        Objects.requireNonNull(freeCancellationUntil, "freeCancellationUntil");
        boolean deadlinePassed = reason.filter(NonRefundableReason.DEADLINE_PASSED::equals).isPresent();
        if (deadlinePassed != deadlinePassedAppointment.isPresent()
                || deadlinePassed != freeCancellationUntil.isPresent()) {
            throw new IllegalArgumentException(
                    "The appointment and its free-cancellation time come with DEADLINE_PASSED, and only with it.");
        }
    }

    /**
     * The rule at {@code asOf}, for the case's booked appointments (not the cancelled ones: a credit refunded by a
     * cancellation in time doesn't count) and its forfeited credits. A running appointment is consumed. An appointment
     * that hasn't started is late from its free-cancellation time on, that moment included: exactly when cancelling it
     * would forfeit its credit, as {@link Appointment}'s cancellation rules decide (UC-07), so a case cancelled
     * refund-eligible cancels no appointment late.
     */
    public static RefundEligibility at(List<Appointment> bookedAppointments, int forfeitedCredits,
            CancellationDeadline deadline, Instant asOf) {
        Objects.requireNonNull(bookedAppointments, "bookedAppointments");
        Objects.requireNonNull(deadline, "deadline");
        Objects.requireNonNull(asOf, "asOf");
        if (forfeitedCredits < 0) {
            throw new IllegalArgumentException("The forfeited credits can't be negative.");
        }
        if (bookedAppointments.stream().anyMatch(a -> !a.isCancellableAt(asOf))) {
            return withReason(NonRefundableReason.SESSION_TOOK_PLACE);
        }
        if (forfeitedCredits > 0) {
            return withReason(NonRefundableReason.CREDIT_FORFEITED);
        }
        Optional<Appointment> soonestLate = bookedAppointments.stream()
                .filter(a -> !a.isCreditRefundedIfCancelledAt(deadline, asOf))
                .min(Comparator.comparing(Appointment::start));
        if (soonestLate.isPresent()) {
            Appointment late = soonestLate.get();
            return new RefundEligibility(Optional.of(NonRefundableReason.DEADLINE_PASSED), Optional.of(late),
                    Optional.of(late.freeCancellationUntil(deadline)));
        }
        return new RefundEligibility(Optional.empty(), Optional.empty(), Optional.empty());
    }

    public boolean isRefundEligible() {
        return reason.isEmpty();
    }

    private static RefundEligibility withReason(NonRefundableReason reason) {
        return new RefundEligibility(Optional.of(reason), Optional.empty(), Optional.empty());
    }
}
