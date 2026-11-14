package com.cleanlearning.api.consultation.request;

import jakarta.validation.constraints.NotNull;

public record CancelConsultationAppointmentRequest(@NotNull Boolean creditRefunded) {}
