package com.cleanlearning.adapters.database.offering.row;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * A published consultation offering's own fields, without its packages and experts. The image id is {@code null}
 * without an image.
 */
public record ShopConsultationOfferingRow(UUID id, String name, String description, @Nullable UUID imageId,
        int sessionDurationMinutes, int cancellationDeadlineHours) {
}
