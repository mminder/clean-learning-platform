package com.cleanlearning.domain.offering.value;

import com.cleanlearning.domain.shared.InvalidValueException;
import java.time.Duration;
import java.time.Instant;

/** Zero means until the session starts. */
public record CancellationDeadline(int hours) {

  public CancellationDeadline {
    if (hours < 0) {
      throw new InvalidValueException("A cancellation deadline is not negative: " + hours);
    }
  }

  /** Real hours, not wall-clock (D-11). */
  public Instant freeUntil(Instant start) {
    return start.minus(Duration.ofHours(hours));
  }
}
