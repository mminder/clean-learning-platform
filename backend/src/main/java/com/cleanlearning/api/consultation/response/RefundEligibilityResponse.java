package com.cleanlearning.api.consultation.response;

import com.cleanlearning.domain.consultation.RefundEligibility;
import com.cleanlearning.domain.consultation.value.NonRefundableReason;
import org.jspecify.annotations.Nullable;

/**
 * {@code reason} is {@code null} when it's refund-eligible; {@code appointment} comes only with
 * {@code DEADLINE_PASSED}.
 */
public record RefundEligibilityResponse(
    boolean refundEligible,
    @Nullable NonRefundableReason reason,
    @Nullable DeadlinePassedAppointmentResponse appointment) {

  public static RefundEligibilityResponse from(RefundEligibility eligibility) {
    return new RefundEligibilityResponse(
        eligibility.isRefundEligible(),
        eligibility.reason().orElse(null),
        eligibility
            .deadlinePassedAppointment()
            .map(appointment ->
                new DeadlinePassedAppointmentResponse(
                    appointment.start(),
                    appointment.end(),
                    eligibility.freeCancellationUntil().orElseThrow()))
            .orElse(null));
  }
}
