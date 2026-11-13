package com.cleanlearning.domain.availability.value;

import java.time.LocalTime;
import java.util.Objects;

/**
 * A daily time range of an expert's availability (UC-13): Zurich wall-clock times, starting before
 * it ends, at any minute. The column's check repeats the rule (D-36).
 */
public record TimeRange(LocalTime start, LocalTime end) {

  public TimeRange {
    Objects.requireNonNull(start, "start");
    Objects.requireNonNull(end, "end");
    if (!start.isBefore(end)) {
      throw new IllegalArgumentException(
          "A time range ends after it starts: " + start + "–" + end + ".");
    }
  }

  /**
   * Whether the two ranges share a time, or one starts where the other ends (spec 04, decision 9).
   */
  public boolean overlapsOrTouches(TimeRange other) {
    return !start.isAfter(other.end) && !other.start.isAfter(end);
  }

  @Override
  public String toString() {
    return start + "–" + end;
  }
}
