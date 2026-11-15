package com.cleanlearning.api.user.response;

import com.cleanlearning.domain.user.OwnExpertProfile;
import com.cleanlearning.domain.user.value.TeamsLink;
import org.jspecify.annotations.Nullable;

/** The expert's own profile, the only answer with their Teams link apart from their cases. */
public record ExpertProfileResponse(String displayName, String bio, @Nullable String teamsLink) {

  public static ExpertProfileResponse from(OwnExpertProfile profile) {
    return new ExpertProfileResponse(
        profile.profile().displayName().value(),
        profile.profile().bio(),
        profile.teamsLink().map(TeamsLink::value).orElse(null));
  }
}
