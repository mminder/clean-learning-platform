package com.cleanlearning.api.availability.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * The body of {@code PUT}: the availability page's form (UC-13 steps 3–4). Dates are Zurich
 * calendar dates, a date named twice counting once; the time ranges are Zurich wall-clock times.
 * Either list may be empty. The annotations repeat UC-13's rules, so a 400 names every invalid
 * range ({@link ValidTimeRanges}); the domain stays the authority, and also refuses a date before
 * today.
 */
@ValidTimeRanges
public record AvailabilityRequest(
    @NotNull List<@NotNull LocalDate> dates,
    @NotNull List<@NotNull @Valid TimeRangeRequest> timeRanges) {}
