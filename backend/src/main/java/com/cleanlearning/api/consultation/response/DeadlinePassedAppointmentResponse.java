package com.cleanlearning.api.consultation.response;

import java.time.Instant;

public record DeadlinePassedAppointmentResponse(
    Instant start, Instant end, Instant freeCancellationUntil) {}
