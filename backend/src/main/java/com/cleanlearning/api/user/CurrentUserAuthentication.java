package com.cleanlearning.api.user;

import com.cleanlearning.domain.user.CurrentUser;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * The roles are the authorities {@code ROLE_CUSTOMER}, {@code ROLE_EXPERT} and {@code ROLE_ADMIN},
 * which {@code SecurityConfig} and {@code @PreAuthorize} check.
 */
public final class CurrentUserAuthentication {

  private CurrentUserAuthentication() {}

  public static Authentication of(CurrentUser currentUser) {
    List<SimpleGrantedAuthority> authorities =
        currentUser.roles().stream()
            .sorted()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
            .toList();
    return UsernamePasswordAuthenticationToken.authenticated(currentUser, null, authorities);
  }
}
