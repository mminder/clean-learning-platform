package com.cleanlearning.adapters.database.consultation.row;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record CustomerConsultationCaseSummaryRow(
    UUID id,
    String offeringName,
    @Nullable UUID offeringImageId,
    int sessionCount,
    Instant purchasedAt,
    @Nullable String expertDisplayName,
    @Nullable Instant cancelledAt) {}
