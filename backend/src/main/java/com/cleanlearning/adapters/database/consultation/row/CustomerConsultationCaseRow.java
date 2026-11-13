package com.cleanlearning.adapters.database.consultation.row;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.cleanlearning.domain.consultation.value.CaseStatus;

/**
 * A case with its offering's name and image, read live, its assigned expert's public profile fields, and its status
 * with its cancellation. The image id is {@code null} without an image; the expert's fields are {@code null} until the
 * first booking; {@code cancelledAt} and {@code refundEligible} are {@code null} while the case is {@code ACTIVE}.
 */
public record CustomerConsultationCaseRow(UUID id, String offeringName, @Nullable UUID offeringImageId,
        BigDecimal pricePaid, int sessionCount, int sessionDurationMinutes, int cancellationDeadlineHours,
        Instant purchasedAt, @Nullable UUID expertId, @Nullable String expertDisplayName, @Nullable String expertBio,
        CaseStatus status, @Nullable Instant cancelledAt, @Nullable Boolean refundEligible) {
}
