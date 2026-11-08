package com.cleanlearning.adapters.database.offering;

import java.util.UUID;

/**
 * A published consultation offering's own fields, without its packages and experts.
 */
record ShopConsultationOfferingRow(UUID id, String name, String description, int sessionDurationMinutes,
        int cancellationDeadlineHours) {
}
