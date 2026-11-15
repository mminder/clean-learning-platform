package com.cleanlearning.adapters.database.user;

import com.cleanlearning.adapters.database.user.entity.ExpertProfileEntity;
import com.cleanlearning.application.user.port.LoadOwnExpertProfilePort;
import com.cleanlearning.application.user.port.SaveOwnExpertProfilePort;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.user.ExpertProfile;
import com.cleanlearning.domain.user.OwnExpertProfile;
import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.TeamsLink;
import com.cleanlearning.domain.user.value.UserId;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** The expert's own profile, with the Teams link: only the profile's use cases read it. */
@Component
class ExpertProfileAdapter implements LoadOwnExpertProfilePort, SaveOwnExpertProfilePort {

  private final EntityManager entityManager;

  ExpertProfileAdapter(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public OwnExpertProfile load(UserId expertId) {
    ExpertProfileEntity entity = find(expertId);
    return new OwnExpertProfile(
        new ExpertProfile(
            expertId,
            new DisplayName(entity.getDisplayName()),
            entity.getBio(),
            Optional.ofNullable(entity.getPhotoId()).map(ImageId::new)),
        Optional.ofNullable(entity.getTeamsLink()).map(TeamsLink::new));
  }

  /** Changes the managed entity that {@link #load} read in the same transaction. */
  @Override
  public void save(OwnExpertProfile profile) {
    ExpertProfileEntity entity = find(profile.profile().id());
    entity.setDisplayName(profile.profile().displayName().value());
    entity.setBio(profile.profile().bio());
    entity.setTeamsLink(profile.teamsLink().map(TeamsLink::value).orElse(null));
    entity.setPhotoId(profile.profile().photoId().map(ImageId::value).orElse(null));
  }

  private ExpertProfileEntity find(UserId expertId) {
    ExpertProfileEntity entity = entityManager.find(ExpertProfileEntity.class, expertId.value());
    if (entity == null) {
      throw new IllegalStateException("Expert profile " + expertId.value() + " not found.");
    }
    return entity;
  }
}
