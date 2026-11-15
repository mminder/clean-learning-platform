package com.cleanlearning.domain.user;

import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.UserId;
import java.util.Objects;
import java.util.Optional;

/** The public profile: what the shop, the expert picker and the customer's case show (UC-14). */
public record ExpertProfile(
    UserId id, DisplayName displayName, String bio, Optional<ImageId> photoId) {

  public ExpertProfile {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(displayName, "displayName");
    Objects.requireNonNull(bio, "bio");
    Objects.requireNonNull(photoId, "photoId");
  }
}
