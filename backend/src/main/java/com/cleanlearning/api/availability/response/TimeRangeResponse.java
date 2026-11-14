package com.cleanlearning.api.availability.response;

import com.cleanlearning.domain.availability.value.TimeRange;
import java.time.LocalTime;

public record TimeRangeResponse(LocalTime start, LocalTime end) {

  public static TimeRangeResponse from(TimeRange range) {
    return new TimeRangeResponse(range.start(), range.end());
  }
}
