package com.cleanlearning.adapters.database.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class UserEntity {

  @Id private UUID id;

  private String name;

  private String email;

  protected UserEntity() {
    // for JPA
  }
}
