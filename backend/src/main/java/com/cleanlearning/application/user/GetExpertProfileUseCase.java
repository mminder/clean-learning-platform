package com.cleanlearning.application.user;

import com.cleanlearning.application.user.port.LoadOwnExpertProfilePort;
import com.cleanlearning.domain.user.CurrentUser;
import com.cleanlearning.domain.user.OwnExpertProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetExpertProfileUseCase {

  private final LoadOwnExpertProfilePort loadOwnExpertProfile;

  GetExpertProfileUseCase(LoadOwnExpertProfilePort loadOwnExpertProfile) {
    this.loadOwnExpertProfile = loadOwnExpertProfile;
  }

  /** The current user's own profile, with the Teams link (UC-14). */
  @Transactional(readOnly = true)
  public OwnExpertProfile invoke(CurrentUser user) {
    return loadOwnExpertProfile.load(user.id());
  }
}
