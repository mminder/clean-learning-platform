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

    /**
     * Foreign key to {@link com.cleanlearning.adapters.database.user.entity.ExpertProfileEntity}, whose id is also the
     * {@link com.cleanlearning.adapters.database.user.entity.UserEntity}'s id.
     */
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
