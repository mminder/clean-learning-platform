package com.cleanlearning.api.user.response;

import com.cleanlearning.domain.user.Expert;
import java.util.UUID;

public record ExpertResponse(UUID id, String displayName) {

  public static ExpertResponse from(Expert expert) {
    return new ExpertResponse(expert.id().value(), expert.displayName().value());
  }
}
