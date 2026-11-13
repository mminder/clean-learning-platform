package com.cleanlearning.domain.consultation.value;

import java.time.Instant;
import java.util.Objects;

/**
 * What a cancelled case stores of its cancellation (UC-08 step 4, spec 05 decision 11): when it was
 * cancelled, from the application's clock, and whether its purchase price would be refunded. No
 * money moves in the PoC.
 */
public record CancellationOutcome(Instant cancelledAt, boolean refundEligible) {

  public CancellationOutcome {
    Objects.requireNonNull(cancelledAt, "cancelledAt");
  }
}
