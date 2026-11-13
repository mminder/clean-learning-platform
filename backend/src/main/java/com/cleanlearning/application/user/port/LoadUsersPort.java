package com.cleanlearning.application.user.port;

import com.cleanlearning.domain.user.User;
import java.util.List;

public interface LoadUsersPort {

  /** Every user with their roles, sorted by name. */
  List<User> loadAll();
}
