package com.cleanlearning.api.consultation.request;

import jakarta.validation.constraints.NotNull;

/**
 * The body of a case's cancellation (UC-08 step 3): whether the dialog said the purchase price
 * would be refunded. The server cancels only with this outcome; another one answers 409 {@code
 * REFUND_ELIGIBILITY_CHANGED} (spec 05, decision 4).
 */
public record CancelConsultationCaseRequest(@NotNull Boolean refundEligible) {}
