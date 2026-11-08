package com.cleanlearning.api.user;

import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.cleanlearning.domain.user.CurrentUser;

/**
 * The authentication a login keeps in the session: the {@link CurrentUser} as the principal, which controllers take
 * with {@code @AuthenticationPrincipal}, and its roles as the authorities {@code ROLE_CUSTOMER}, {@code ROLE_EXPERT}
 * and {@code ROLE_ADMIN}, which {@code SecurityConfig} checks.
 */
public final class CurrentUserAuthentication {

    private CurrentUserAuthentication() {
    }

    public static Authentication of(CurrentUser currentUser) {
        List<SimpleGrantedAuthority> authorities = currentUser.roles().stream()
                .sorted()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
        return UsernamePasswordAuthenticationToken.authenticated(currentUser, null, authorities);
    }
}
