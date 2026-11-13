package com.cleanlearning.domain.consultation;

import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.UserId;
import java.util.Objects;

/**
 * A case's assigned expert, as its customer sees them (UC-05's case detail): the public fields of
 * their expert profile. The first booking assigns them, and they stay after they stop being
 * eligible (UC-06).
 */
public record AssignedExpert(UserId id, DisplayName displayName, String bio) {

  public AssignedExpert {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(displayName, "displayName");
    Objects.requireNonNull(bio, "bio");
  }
}
