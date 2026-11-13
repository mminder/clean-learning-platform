package com.cleanlearning.api.availability.response;

import com.cleanlearning.domain.availability.Availability;
import java.time.LocalDate;
import java.util.List;

/**
 * The expert's own availability (UC-13): their Zurich dates from today on, ascending, and their
 * daily time ranges, by start.
 */
public record AvailabilityResponse(List<LocalDate> dates, List<TimeRangeResponse> timeRanges) {

  public static AvailabilityResponse from(Availability availability) {
    return new AvailabilityResponse(
        List.copyOf(availability.dates()),
        availability.timeRanges().stream().map(TimeRangeResponse::from).toList());
  }
}
