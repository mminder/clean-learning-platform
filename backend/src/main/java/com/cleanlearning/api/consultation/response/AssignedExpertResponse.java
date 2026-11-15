package com.cleanlearning.api.consultation.response;

import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.user.ExpertProfile;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record AssignedExpertResponse(
    UUID id, String displayName, String bio, @Nullable UUID photoId) {

  public static AssignedExpertResponse from(ExpertProfile expert) {
    return new AssignedExpertResponse(
        expert.id().value(),
        expert.displayName().value(),
        expert.bio(),
        expert.photoId().map(ImageId::value).orElse(null));
  }
}
