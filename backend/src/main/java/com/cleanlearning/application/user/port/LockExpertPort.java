package com.cleanlearning.application.user.port;

import com.cleanlearning.domain.user.value.UserId;

public interface LockExpertPort {

  /**
   * Locks the expert's user row until the transaction ends, waiting for every other lock of the
   * row. Saving an availability and a booking take it (D-27: offering → case → expert → run), so
   * two saves, or a save and a booking, of one expert run one after the other.
   *
   * @throws IllegalStateException if there's no user with this id: the caller always names an
   *     existing one
   */
  void lock(UserId expertId);
}
