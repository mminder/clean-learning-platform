package com.cleanlearning.api.availability.request;

import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

/** A daily time range of the availability page (UC-13): Zurich wall-clock times, such as {@code 09:00}. */
public record TimeRangeRequest(@NotNull LocalTime start, @NotNull LocalTime end) {
}
