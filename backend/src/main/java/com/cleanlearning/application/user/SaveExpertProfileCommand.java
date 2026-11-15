package com.cleanlearning.application.user;

import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.TeamsLink;
import java.util.Objects;
import java.util.Optional;

/** The bio may be empty, and an empty {@code teamsLink} removes the link. */
public record SaveExpertProfileCommand(
    DisplayName displayName, String bio, Optional<TeamsLink> teamsLink) {

  public SaveExpertProfileCommand {
    Objects.requireNonNull(displayName, "displayName");
    Objects.requireNonNull(bio, "bio");
    Objects.requireNonNull(teamsLink, "teamsLink");
  }
}
