package com.cleanlearning.adapters.database.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "expert_profile")
public class ExpertProfileEntity {

  @Id private UUID userId;

  private String displayName;

  private String bio;

  // Null without a link.
  private @Nullable String teamsLink;

  // Null without a photo.
  private @Nullable UUID photoId;

  protected ExpertProfileEntity() {}

  public UUID getUserId() {
    return userId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getBio() {
    return bio;
  }

  public @Nullable String getTeamsLink() {
    return teamsLink;
  }

  public @Nullable UUID getPhotoId() {
    return photoId;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  public void setTeamsLink(@Nullable String teamsLink) {
    this.teamsLink = teamsLink;
  }

  public void setPhotoId(@Nullable UUID photoId) {
    this.photoId = photoId;
  }
}
