package com.cleanlearning.adapters.database.offering.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "eligible_expert")
@IdClass(EligibleExpertEntity.Key.class)
public class EligibleExpertEntity {

  @Id private UUID consultationOfferingId;

  @Id private UUID expertId;

  protected EligibleExpertEntity() {}

  public EligibleExpertEntity(UUID consultationOfferingId, UUID expertId) {
    this.consultationOfferingId = consultationOfferingId;
    this.expertId = expertId;
  }

  public UUID getExpertId() {
    return expertId;
  }

  record Key(UUID consultationOfferingId, UUID expertId) {}
}
