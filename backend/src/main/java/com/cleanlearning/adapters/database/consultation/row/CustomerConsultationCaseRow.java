package com.cleanlearning.adapters.database.consultation.row;

import com.cleanlearning.domain.consultation.value.CaseStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record CustomerConsultationCaseRow(
    UUID id,
    String offeringName,
    @Nullable UUID offeringImageId,
    BigDecimal pricePaid,
    int sessionCount,
    int sessionDurationMinutes,
    int cancellationDeadlineHours,
    Instant purchasedAt,
    @Nullable UUID expertId,
    @Nullable String expertDisplayName,
    @Nullable String expertBio,
    CaseStatus status,
    @Nullable Instant cancelledAt,
    @Nullable Boolean refundEligible) {}
