package com.cleanlearning.api.availability.response;

import com.cleanlearning.domain.availability.Availability;
import java.time.LocalDate;
import java.util.List;

public record AvailabilityResponse(List<LocalDate> dates, List<TimeRangeResponse> timeRanges) {

  public static AvailabilityResponse from(Availability availability) {
    return new AvailabilityResponse(
        List.copyOf(availability.dates()),
        availability.timeRanges().stream().map(TimeRangeResponse::from).toList());
  }
}
