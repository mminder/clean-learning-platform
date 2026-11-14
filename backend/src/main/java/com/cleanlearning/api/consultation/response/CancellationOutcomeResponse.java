package com.cleanlearning.api.consultation.response;

import com.cleanlearning.domain.consultation.value.CancellationOutcome;
import java.time.Instant;

public record CancellationOutcomeResponse(Instant cancelledAt, boolean refundEligible) {

  public static CancellationOutcomeResponse from(CancellationOutcome outcome) {
    return new CancellationOutcomeResponse(outcome.cancelledAt(), outcome.refundEligible());
  }
}
