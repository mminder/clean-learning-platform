package com.cleanlearning.domain.offering;

import java.util.Objects;

import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.OfferingStatus;

/**
 * A consultation offering as a row in the admin's list (UC-16 step 1).
 */
public record ConsultationOfferingSummary(OfferingId id, OfferingName name, OfferingStatus status, int packageCount,
        int eligibleExpertCount) {

    public ConsultationOfferingSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(status, "status");
        if (packageCount < 0) {
            throw new IllegalArgumentException("The package count can't be negative: " + packageCount);
        }
        if (eligibleExpertCount < 0) {
            throw new IllegalArgumentException("The eligible expert count can't be negative: " + eligibleExpertCount);
        }
    }
}
