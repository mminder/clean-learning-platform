package com.cleanlearning.adapters.database.consultation.row;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A case on the list, with its offering's name and image, read live, and its assigned expert's
 * display name. The image id is {@code null} without an image; the expert's name is {@code null}
 * until the first booking; {@code cancelledAt} is {@code null} while the case is {@code ACTIVE}.
 */
public record CustomerConsultationCaseSummaryRow(
    UUID id,
    String offeringName,
    @Nullable UUID offeringImageId,
    int sessionCount,
    Instant purchasedAt,
    @Nullable String expertDisplayName,
    @Nullable Instant cancelledAt) {}
