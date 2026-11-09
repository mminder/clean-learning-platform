package com.cleanlearning.api.consultation.response;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.cleanlearning.domain.consultation.CustomerConsultationCase;
import com.cleanlearning.domain.image.value.ImageId;

/**
 * A case as its customer sees it (UC-05's case detail). The offering's name and image are live; the image id is
 * {@code null} without an image, and {@code GET /api/images/{id}} serves it. {@code purchasedAt} is an instant.
 */
public record CustomerConsultationCaseResponse(UUID id, String offeringName, @Nullable UUID offeringImageId,
        PurchaseTermsResponse purchaseTerms, Instant purchasedAt, int openCredits, int totalCredits) {

    public static CustomerConsultationCaseResponse from(CustomerConsultationCase consultationCase) {
        return new CustomerConsultationCaseResponse(consultationCase.id().value(),
                consultationCase.offeringName().value(),
                consultationCase.offeringImageId().map(ImageId::value).orElse(null),
                PurchaseTermsResponse.from(consultationCase.purchaseTerms()), consultationCase.purchasedAt(),
                consultationCase.openCredits(), consultationCase.totalCredits());
    }
}
