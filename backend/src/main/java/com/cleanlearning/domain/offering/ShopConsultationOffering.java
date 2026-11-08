package com.cleanlearning.domain.offering;

import java.util.List;
import java.util.Objects;

import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingType;
import com.cleanlearning.domain.offering.value.SessionDuration;

/**
 * A consultation offering as its detail page in the shop shows it (UC-02 step 4): the packages, fewest sessions first,
 * and the eligible experts, by display name. The shop shows only an offering with at least one package; it may have
 * no eligible experts.
 */
public record ShopConsultationOffering(OfferingId id, String name, String description,
        SessionDuration sessionDuration, CancellationDeadline cancellationDeadline, List<ConsultationPackage> packages,
        List<EligibleExpert> eligibleExperts) {

    public ShopConsultationOffering {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(sessionDuration, "sessionDuration");
        Objects.requireNonNull(cancellationDeadline, "cancellationDeadline");
        packages = List.copyOf(packages);
        if (packages.isEmpty()) {
            throw new IllegalArgumentException("A consultation offering needs at least one package: " + id);
        }
        eligibleExperts = List.copyOf(eligibleExperts);
    }

    public OfferingType type() {
        return OfferingType.CONSULTATION;
    }
}
