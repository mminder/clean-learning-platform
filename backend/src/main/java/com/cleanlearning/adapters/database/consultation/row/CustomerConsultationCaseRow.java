package com.cleanlearning.adapters.database.consultation.row;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * A case with its offering's name and image, read live. The image id is {@code null} without an image.
 */
public record CustomerConsultationCaseRow(UUID id, String offeringName, @Nullable UUID offeringImageId,
        BigDecimal pricePaid, int sessionCount, int sessionDurationMinutes, int cancellationDeadlineHours,
        Instant purchasedAt) {
}
