package com.cleanlearning.adapters.database.offering.row;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record ShopConsultationOfferingRow(
    UUID id,
    String name,
    String description,
    @Nullable UUID imageId,
    int sessionDurationMinutes,
    int cancellationDeadlineHours) {}
