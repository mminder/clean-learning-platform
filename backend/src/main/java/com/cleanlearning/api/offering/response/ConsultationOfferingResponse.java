package com.cleanlearning.api.offering.response;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.cleanlearning.api.response.ConsultationPackageResponse;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingStatus;
import com.cleanlearning.domain.user.value.UserId;

/**
 * A consultation offering as the admin edits it. The packages come fewest sessions first, then by id; the eligible
 * expert ids are sorted by id. The image id is {@code null} without an image; {@code GET /api/images/{id}} serves it.
 */
public record ConsultationOfferingResponse(UUID id, String name, String description, OfferingStatus status,
        int sessionDurationMinutes, int cancellationDeadlineHours, List<ConsultationPackageResponse> packages,
        List<UUID> eligibleExpertIds, @Nullable UUID imageId) {

    // Sorted as text, which is the database's uuid order; UUID.compareTo compares signed numbers.
    private static final Comparator<UUID> BY_TEXT = Comparator.comparing(UUID::toString);

    public static ConsultationOfferingResponse from(ConsultationOffering offering) {
        return new ConsultationOfferingResponse(offering.id().value(), offering.name().value(),
                offering.description(), offering.status(), offering.sessionDuration().minutes(),
                offering.cancellationDeadline().hours(),
                offering.packages().stream().map(ConsultationPackageResponse::from).toList(),
                offering.eligibleExpertIds().stream().map(UserId::value).sorted(BY_TEXT).toList(),
                offering.imageId().map(ImageId::value).orElse(null));
    }
}
