package com.cleanlearning.api.consultation.request;

import jakarta.validation.constraints.NotNull;

public record CancelConsultationCaseRequest(@NotNull Boolean refundEligible) {}
