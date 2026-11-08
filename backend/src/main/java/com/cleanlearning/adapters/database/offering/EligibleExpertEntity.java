package com.cleanlearning.adapters.database.offering;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Read through the projection in {@link OfferingRepository}.
 */
@Entity
@Table(name = "eligible_expert")
@IdClass(EligibleExpertEntity.Key.class)
class EligibleExpertEntity {

    @Id
    private UUID consultationOfferingId;

    // The expert's user id, which is also the id of their expert profile.
    @Id
    private UUID expertId;

    protected EligibleExpertEntity() {
        // for JPA
    }

    record Key(UUID consultationOfferingId, UUID expertId) {
    }
}
