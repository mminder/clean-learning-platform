package com.cleanlearning.domain.user;

import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.UserId;
import java.util.Objects;

public record ExpertProfile(UserId id, DisplayName displayName, String bio) {

  public ExpertProfile {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(displayName, "displayName");
    Objects.requireNonNull(bio, "bio");
  }
}
