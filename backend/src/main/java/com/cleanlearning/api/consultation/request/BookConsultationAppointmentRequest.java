package com.cleanlearning.api.consultation.request;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record BookConsultationAppointmentRequest(@NotNull UUID expertId, @NotNull Instant start) {}
