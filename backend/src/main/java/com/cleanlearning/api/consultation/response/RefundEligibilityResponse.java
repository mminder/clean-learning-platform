package com.cleanlearning.api.consultation.response;

import org.jspecify.annotations.Nullable;

import com.cleanlearning.domain.consultation.RefundEligibility;
import com.cleanlearning.domain.consultation.value.NonRefundableReason;

/**
 * Whether cancelling a case now would refund its purchase price (UC-08, D-12), computed by the server: the cancel
 * dialog's preview. {@code reason} is {@code null} when it's refund-eligible, otherwise the first that applies, in
 * UC-08's order; {@code appointment} comes only with {@code DEADLINE_PASSED}, the soonest such appointment.
 */
public record RefundEligibilityResponse(boolean refundEligible, @Nullable NonRefundableReason reason,
        @Nullable DeadlinePassedAppointmentResponse appointment) {

    public static RefundEligibilityResponse from(RefundEligibility eligibility) {
        return new RefundEligibilityResponse(eligibility.isRefundEligible(), eligibility.reason().orElse(null),
                eligibility.deadlinePassedAppointment()
                        .map(appointment -> new DeadlinePassedAppointmentResponse(appointment.start(),
                                appointment.end(), eligibility.freeCancellationUntil().orElseThrow()))
                        .orElse(null));
    }
}
