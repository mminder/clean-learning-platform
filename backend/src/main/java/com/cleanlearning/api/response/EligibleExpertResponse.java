package com.cleanlearning.api.response;

import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.user.ExpertProfile;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record EligibleExpertResponse(
    UUID id, String displayName, String bio, @Nullable UUID photoId) {

  public static EligibleExpertResponse from(ExpertProfile expert) {
    return new EligibleExpertResponse(
        expert.id().value(),
        expert.displayName().value(),
        expert.bio(),
        expert.photoId().map(ImageId::value).orElse(null));
  }
}
