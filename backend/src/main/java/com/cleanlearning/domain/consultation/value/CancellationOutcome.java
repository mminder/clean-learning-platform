package com.cleanlearning.domain.consultation.value;

import java.time.Instant;
import java.util.Objects;

/** No money moves in the PoC. */
public record CancellationOutcome(Instant cancelledAt, boolean refundEligible) {

  public CancellationOutcome {
    Objects.requireNonNull(cancelledAt, "cancelledAt");
  }
}
