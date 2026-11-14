package com.cleanlearning.api.consultation.response;

import com.cleanlearning.domain.user.ExpertProfile;
import java.util.UUID;

public record AssignedExpertResponse(UUID id, String displayName, String bio) {

  public static AssignedExpertResponse from(ExpertProfile expert) {
    return new AssignedExpertResponse(
        expert.id().value(), expert.displayName().value(), expert.bio());
  }
}
