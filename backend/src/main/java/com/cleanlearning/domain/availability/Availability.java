package com.cleanlearning.domain.availability;

import com.cleanlearning.domain.availability.value.TimeRange;
import com.cleanlearning.domain.shared.InvalidValueException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/** The time ranges are sorted by start and neither overlap nor touch. */
public record Availability(SortedSet<LocalDate> dates, List<TimeRange> timeRanges) {

  public Availability {
    dates = Collections.unmodifiableSortedSet(new TreeSet<>(dates));
    timeRanges = timeRanges.stream().sorted(Comparator.comparing(TimeRange::start)).toList();
    // Sorted, so a range that overlaps or touches any earlier one also does so with its
    // predecessor.
    for (int i = 1; i < timeRanges.size(); i++) {
      TimeRange previous = timeRanges.get(i - 1);
      TimeRange range = timeRanges.get(i);
      if (range.overlapsOrTouches(previous)) {
        throw new InvalidValueException(
            "Time ranges must not overlap or touch: " + previous + " and " + range + ".");
      }
    }
  }

  public void requireNoDateBefore(LocalDate today) {
    if (!dates.isEmpty() && dates.first().isBefore(today)) {
      throw new InvalidValueException(
          "A date before today can't be changed: " + dates.first() + ".");
    }
  }
}
