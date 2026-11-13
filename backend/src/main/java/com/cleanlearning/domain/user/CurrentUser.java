package com.cleanlearning.domain.user;

import com.cleanlearning.domain.user.value.Role;
import com.cleanlearning.domain.user.value.UserId;
import java.util.Objects;
import java.util.Set;

/**
 * The logged-in user, as controllers pass it into use cases. The roles are the ones the user had at
 * login; they stay for the session.
 */
public record CurrentUser(UserId id, Set<Role> roles) {

  public CurrentUser {
    Objects.requireNonNull(id, "id");
    roles = Set.copyOf(roles);
  }
}
