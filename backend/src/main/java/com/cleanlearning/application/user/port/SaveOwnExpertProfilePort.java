package com.cleanlearning.application.user.port;

import com.cleanlearning.domain.user.OwnExpertProfile;

public interface SaveOwnExpertProfilePort {

  /** Writes the profile that {@link LoadOwnExpertProfilePort#load} read in the same transaction. */
  void save(OwnExpertProfile profile);
}
