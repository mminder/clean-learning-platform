package com.cleanlearning.api.response;

import com.cleanlearning.domain.user.ExpertProfile;
import java.util.UUID;

public record EligibleExpertResponse(UUID id, String displayName, String bio) {

  public static EligibleExpertResponse from(ExpertProfile expert) {
    return new EligibleExpertResponse(
        expert.id().value(), expert.displayName().value(), expert.bio());
  }
}
