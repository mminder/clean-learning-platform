package com.cleanlearning.adapters.database.availability.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "availability_date")
@IdClass(AvailabilityDateEntity.Key.class)
public class AvailabilityDateEntity {

  @Id private UUID expertId;

  @Id private LocalDate availableOn;

  protected AvailabilityDateEntity() {}

  public AvailabilityDateEntity(UUID expertId, LocalDate availableOn) {
    this.expertId = expertId;
    this.availableOn = availableOn;
  }

  public record Key(UUID expertId, LocalDate availableOn) {}
}
