package com.cleanlearning.domain.availability.value;

import com.cleanlearning.domain.shared.InvalidValueException;
import java.time.LocalTime;
import java.util.Objects;

/** Zurich wall-clock times; the column's check repeats that it starts before it ends (D-36). */
public record TimeRange(LocalTime start, LocalTime end) {

  public TimeRange {
    Objects.requireNonNull(start, "start");
    Objects.requireNonNull(end, "end");
    if (!start.isBefore(end)) {
      throw new InvalidValueException(
          "A time range ends after it starts: " + start + "–" + end + ".");
    }
  }

  public boolean overlapsOrTouches(TimeRange other) {
    return !start.isAfter(other.end) && !other.start.isAfter(end);
  }

  @Override
  public String toString() {
    return start + "–" + end;
  }
}
