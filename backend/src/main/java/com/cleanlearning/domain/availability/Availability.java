package com.cleanlearning.domain.availability;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.cleanlearning.domain.availability.value.TimeRange;

/**
 * An expert's availability (UC-13, D-09): the selected Zurich dates, and one set of daily time ranges that applies to
 * all of them, sorted by start. The ranges neither overlap nor touch (spec 04, decision 9). Either may be empty; then
 * nothing is bookable.
 */
public record Availability(SortedSet<LocalDate> dates, List<TimeRange> timeRanges) {

    public Availability {
        dates = Collections.unmodifiableSortedSet(new TreeSet<>(dates));
        timeRanges = timeRanges.stream().sorted(Comparator.comparing(TimeRange::start)).toList();
        // Sorted by start, a range that overlaps or touches any earlier one also overlaps or touches its predecessor.
        for (int i = 1; i < timeRanges.size(); i++) {
            TimeRange previous = timeRanges.get(i - 1);
            TimeRange range = timeRanges.get(i);
            if (range.overlapsOrTouches(previous)) {
                throw new IllegalArgumentException(
                        "Time ranges must not overlap or touch: " + previous + " and " + range + ".");
            }
        }
    }

    /**
     * Dates before today (Zurich) are past: a save can't change them, while today can (UC-13).
     *
     * @throws IllegalArgumentException naming the earliest date before today
     */
    public void requireNoDateBefore(LocalDate today) {
        if (!dates.isEmpty() && dates.first().isBefore(today)) {
            throw new IllegalArgumentException("A date before today can't be changed: " + dates.first() + ".");
        }
    }
}
