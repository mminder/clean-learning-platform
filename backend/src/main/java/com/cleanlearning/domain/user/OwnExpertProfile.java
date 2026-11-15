package com.cleanlearning.domain.user;

import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.TeamsLink;
import java.util.Objects;
import java.util.Optional;

/**
 * The profile as its expert edits it (UC-14): the public {@link ExpertProfile} and the Teams link,
 * which only the profile's own use cases load (spec 06, decision 15).
 */
public record OwnExpertProfile(ExpertProfile profile, Optional<TeamsLink> teamsLink) {

  public OwnExpertProfile {
    Objects.requireNonNull(profile, "profile");
    Objects.requireNonNull(teamsLink, "teamsLink");
  }

  /** Replaces the display name, the bio and the link; the bio may be empty, and no link is none. */
  public OwnExpertProfile edit(DisplayName displayName, String bio, Optional<TeamsLink> teamsLink) {
    return new OwnExpertProfile(new ExpertProfile(profile.id(), displayName, bio), teamsLink);
  }
}
