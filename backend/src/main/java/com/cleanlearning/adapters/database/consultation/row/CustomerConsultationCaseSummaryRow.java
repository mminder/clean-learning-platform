package com.cleanlearning.adapters.database.consultation.row;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * A case on the list, with its offering's name and image, read live. The image id is {@code null} without an image.
 */
public record CustomerConsultationCaseSummaryRow(UUID id, String offeringName, @Nullable UUID offeringImageId,
        int sessionCount, Instant purchasedAt) {
}
