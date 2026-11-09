package com.cleanlearning.api.availability.response;

import java.time.LocalTime;

import com.cleanlearning.domain.availability.value.TimeRange;

/** A daily time range of the expert's availability: Zurich wall-clock times. */
public record TimeRangeResponse(LocalTime start, LocalTime end) {

    public static TimeRangeResponse from(TimeRange range) {
        return new TimeRangeResponse(range.start(), range.end());
    }
}
