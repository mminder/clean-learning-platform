package com.cleanlearning.domain.consultation.value;

import java.time.Instant;
import java.util.Objects;

/**
 * A time when an expert or a customer is busy, such as a booked appointment: the half-open interval
 * from start to end, so another session may start where it ends (D-10, no buffer).
 */
public record BusyTime(Instant start, Instant end) {

  public BusyTime {
    Objects.requireNonNull(start, "start");
    Objects.requireNonNull(end, "end");
    if (!start.isBefore(end)) {
      throw new IllegalArgumentException(
          "Busy time ends after it starts: " + start + " to " + end + ".");
    }
  }
}
