package com.cleanlearning.adapters.database.consultation.row;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * A case with its offering's name and image, read live, and its assigned expert's public profile fields. The image id
 * is {@code null} without an image; the expert's fields are {@code null} until the first booking.
 */
public record CustomerConsultationCaseRow(UUID id, String offeringName, @Nullable UUID offeringImageId,
        BigDecimal pricePaid, int sessionCount, int sessionDurationMinutes, int cancellationDeadlineHours,
        Instant purchasedAt, @Nullable UUID expertId, @Nullable String expertDisplayName, @Nullable String expertBio) {
}
