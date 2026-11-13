package com.cleanlearning.adapters.database.offering.entity;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "eligible_expert")
@IdClass(EligibleExpertEntity.Key.class)
public class EligibleExpertEntity {

    @Id
    private UUID consultationOfferingId;

    // The expert's user id, which is also the id of their expert profile.
    @Id
    private UUID expertId;

    protected EligibleExpertEntity() {
        // for JPA
    }

    public EligibleExpertEntity(UUID consultationOfferingId, UUID expertId) {
        this.consultationOfferingId = consultationOfferingId;
        this.expertId = expertId;
    }

    public UUID getExpertId() {
        return expertId;
    }

    record Key(UUID consultationOfferingId, UUID expertId) {
    }
}
