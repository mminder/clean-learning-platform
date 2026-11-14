package com.cleanlearning.api.consultation.response;

import com.cleanlearning.domain.user.ExpertProfile;
import java.util.UUID;

/** A case's assigned expert with the public fields of their profile (UC-05's case detail). */
public record AssignedExpertResponse(UUID id, String displayName, String bio) {

  public static AssignedExpertResponse from(ExpertProfile expert) {
    return new AssignedExpertResponse(
        expert.id().value(), expert.displayName().value(), expert.bio());
  }
}
