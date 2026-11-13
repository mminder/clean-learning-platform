package com.cleanlearning.api.consultation.response;

import java.time.Instant;

import com.cleanlearning.domain.consultation.value.CancellationOutcome;

/**
 * A cancelled case's cancellation (UC-08 step 5): when it was cancelled, an instant, and whether its purchase price
 * would be refunded.
 */
public record CancellationOutcomeResponse(Instant cancelledAt, boolean refundEligible) {

    public static CancellationOutcomeResponse from(CancellationOutcome outcome) {
        return new CancellationOutcomeResponse(outcome.cancelledAt(), outcome.refundEligible());
    }
}
