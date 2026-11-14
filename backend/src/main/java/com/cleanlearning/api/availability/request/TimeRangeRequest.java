package com.cleanlearning.api.availability.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record TimeRangeRequest(@NotNull LocalTime start, @NotNull LocalTime end) {}
