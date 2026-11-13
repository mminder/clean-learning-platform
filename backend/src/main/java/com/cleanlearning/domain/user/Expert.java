package com.cleanlearning.domain.user;

import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.UserId;
import java.util.Objects;

/**
 * A user with {@code EXPERT} who has an expert profile: the admin can make them eligible for a
 * consultation offering. The id is the user's id, which is also the id of the profile.
 */
public record Expert(UserId id, DisplayName displayName) {

  public Expert {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(displayName, "displayName");
  }
}
