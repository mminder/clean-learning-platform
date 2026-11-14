package com.cleanlearning.adapters.database.consultation.row;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record ExpertConsultationCaseSummaryRow(
    UUID id,
    String customerName,
    String offeringName,
    int sessionCount,
    Instant purchasedAt,
    @Nullable Instant cancelledAt) {}
