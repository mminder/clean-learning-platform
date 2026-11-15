package com.cleanlearning.application.user.port;

import com.cleanlearning.domain.user.OwnExpertProfile;
import com.cleanlearning.domain.user.value.UserId;

public interface LoadOwnExpertProfilePort {

  /**
   * The expert's whole profile, with the Teams link. Every user with {@code EXPERT} has one (spec
   * 06, decision 12): a missing profile is an {@link IllegalStateException}.
   */
  OwnExpertProfile load(UserId expertId);
}
