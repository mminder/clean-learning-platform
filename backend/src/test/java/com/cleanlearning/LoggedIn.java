package com.cleanlearning;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.cleanlearning.api.user.CurrentUserAuthentication;
import com.cleanlearning.domain.user.CurrentUser;
import com.cleanlearning.domain.user.value.Role;
import com.cleanlearning.domain.user.value.UserId;
import java.util.Set;
import java.util.UUID;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * A request by a logged-in user, with the same authentication a PoC login keeps in the session. Use
 * a user from {@link SeedData}.
 */
public final class LoggedIn {

  private LoggedIn() {}

  public static RequestPostProcessor as(UUID userId, Role... roles) {
    return authentication(
        CurrentUserAuthentication.of(new CurrentUser(new UserId(userId), Set.of(roles))));
  }
}
