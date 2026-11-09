package com.cleanlearning.api.consultation.response;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.cleanlearning.domain.consultation.CustomerConsultationCaseSummary;
import com.cleanlearning.domain.image.value.ImageId;

/**
 * A case on "My purchases" (UC-05). The offering's name and image are live; the image id is {@code null} without an
 * image, and {@code GET /api/images/{id}} serves it. {@code purchasedAt} is an instant.
 */
public record CustomerConsultationCaseSummaryResponse(UUID id, String offeringName,
        @Nullable UUID offeringImageId, Instant purchasedAt, int openCredits, int totalCredits) {

    public static CustomerConsultationCaseSummaryResponse from(CustomerConsultationCaseSummary consultationCase) {
        return new CustomerConsultationCaseSummaryResponse(consultationCase.id().value(),
                consultationCase.offeringName().value(),
                consultationCase.offeringImageId().map(ImageId::value).orElse(null), consultationCase.purchasedAt(),
                consultationCase.openCredits(), consultationCase.totalCredits());
    }
}
