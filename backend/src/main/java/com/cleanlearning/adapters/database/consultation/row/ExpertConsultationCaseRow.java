package com.cleanlearning.adapters.database.consultation.row;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record ExpertConsultationCaseRow(
    UUID id,
    String customerName,
    String customerEmail,
    String offeringName,
    int sessionCount,
    int sessionDurationMinutes,
    int cancellationDeadlineHours,
    Instant purchasedAt,
    @Nullable Instant cancelledAt) {}
