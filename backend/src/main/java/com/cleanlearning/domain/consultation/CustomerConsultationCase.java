package com.cleanlearning.domain.consultation;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.offering.value.OfferingName;

/**
 * A case as its customer sees it (UC-05's case detail): the offering's name and image, read live (D-08), the purchase
 * terms, the purchase time and the credits. Every case is active and unassigned until UC-06 and UC-08 exist.
 */
public record CustomerConsultationCase(ConsultationCaseId id, OfferingName offeringName,
        Optional<ImageId> offeringImageId, PurchaseTerms purchaseTerms, Instant purchasedAt) {

    public CustomerConsultationCase {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(offeringName, "offeringName");
        Objects.requireNonNull(offeringImageId, "offeringImageId");
        Objects.requireNonNull(purchaseTerms, "purchaseTerms");
        Objects.requireNonNull(purchasedAt, "purchasedAt");
    }

    /** The purchase terms' session count, until credit adjustments exist (UC-18). */
    public int totalCredits() {
        return purchaseTerms.sessionCount().value();
    }

    /** Computed, never stored (D-11): every credit is open until appointments exist (UC-06). */
    public int openCredits() {
        return totalCredits();
    }
}
