package com.cleanlearning.adapters.database.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "expert_profile")
public class ExpertProfileEntity {

  @Id private UUID userId;

  private String displayName;

  private String bio;

  protected ExpertProfileEntity() {}
}
