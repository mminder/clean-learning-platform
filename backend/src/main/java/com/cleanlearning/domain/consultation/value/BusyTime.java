package com.cleanlearning.domain.consultation.value;

import java.time.Instant;
import java.util.Objects;

/** Half-open, so another session may start where it ends. */
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
