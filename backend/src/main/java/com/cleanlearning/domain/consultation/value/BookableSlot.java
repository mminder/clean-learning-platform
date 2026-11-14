package com.cleanlearning.domain.consultation.value;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * {@code freeCancellationUntil} is empty when that time has already passed, as with a cancellation
 * deadline longer than the minimum notice (D-11).
 */
public record BookableSlot(Instant start, Instant end, Optional<Instant> freeCancellationUntil) {

  public BookableSlot {
    Objects.requireNonNull(start, "start");
    Objects.requireNonNull(end, "end");
    Objects.requireNonNull(freeCancellationUntil, "freeCancellationUntil");
    if (!start.isBefore(end)) {
      throw new IllegalArgumentException(
          "A bookable slot ends after it starts: " + start + " to " + end + ".");
    }
    if (freeCancellationUntil.isPresent() && freeCancellationUntil.get().isAfter(start)) {
      throw new IllegalArgumentException(
          "Free cancellation ends by the start: "
              + freeCancellationUntil.get()
              + " after "
              + start
              + ".");
    }
  }
}
