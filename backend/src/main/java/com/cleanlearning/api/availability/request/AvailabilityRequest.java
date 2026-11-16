package com.cleanlearning.api.availability.request;

import com.cleanlearning.application.availability.SaveAvailabilityUseCase;
import com.cleanlearning.domain.availability.value.TimeRange;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

/**
 * The annotations repeat UC-13's rules so a 400 names every invalid range; the domain stays the
 * authority.
 */
@ValidTimeRanges
public record AvailabilityRequest(
    @NotNull List<@NotNull LocalDate> dates,
    @NotNull List<@NotNull @Valid TimeRangeRequest> timeRanges) {

  public SaveAvailabilityUseCase.Command toCommand() {
    return new SaveAvailabilityUseCase.Command(
        new HashSet<>(dates),
        timeRanges.stream().map(range -> new TimeRange(range.start(), range.end())).toList());
  }
}
