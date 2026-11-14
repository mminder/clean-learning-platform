package com.cleanlearning.domain.user;

import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.UserId;
import java.util.Objects;

/** The id is the user's id, which is also the id of the profile. */
public record Expert(UserId id, DisplayName displayName) {

  public Expert {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(displayName, "displayName");
  }
}
