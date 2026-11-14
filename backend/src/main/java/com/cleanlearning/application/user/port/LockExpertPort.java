package com.cleanlearning.application.user.port;

import com.cleanlearning.domain.user.value.UserId;

public interface LockExpertPort {

  /**
   * Saving an availability and a booking take it (D-27), so two saves, or a save and a booking, of
   * one expert run one after the other.
   */
  void lock(UserId expertId);
}
