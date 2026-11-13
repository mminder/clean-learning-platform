package com.cleanlearning.application.availability;

import com.cleanlearning.domain.availability.value.TimeRange;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * What the expert saves on their availability page (UC-13 steps 3–4): the selected dates, a date
 * named twice counting once, and the daily time ranges.
 */
public record SaveAvailabilityCommand(Set<LocalDate> dates, List<TimeRange> timeRanges) {

  public SaveAvailabilityCommand {
    dates = Set.copyOf(dates);
    timeRanges = List.copyOf(timeRanges);
  }
}
