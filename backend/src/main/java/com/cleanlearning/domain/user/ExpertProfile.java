package com.cleanlearning.domain.user;

import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.UserId;
import java.util.Objects;

/**
 * The public fields of an expert's profile, as customers see them. Whether the expert is eligible
 * or assigned is said by the field that holds it.
 */
public record ExpertProfile(UserId id, DisplayName displayName, String bio) {

  public ExpertProfile {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(displayName, "displayName");
    Objects.requireNonNull(bio, "bio");
  }
}
