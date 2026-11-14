package com.cleanlearning.application.availability;

import com.cleanlearning.domain.availability.value.TimeRange;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record SaveAvailabilityCommand(Set<LocalDate> dates, List<TimeRange> timeRanges) {

  public SaveAvailabilityCommand {
    dates = Set.copyOf(dates);
    timeRanges = List.copyOf(timeRanges);
  }
}
