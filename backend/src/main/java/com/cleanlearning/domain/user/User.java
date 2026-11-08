package com.cleanlearning.domain.user;

import java.util.Objects;
import java.util.Set;

import com.cleanlearning.domain.user.value.Role;
import com.cleanlearning.domain.user.value.UserId;

/**
 * A user with their roles. The name and the email have no rule of their own yet (D-36).
 */
public record User(UserId id, String name, String email, Set<Role> roles) {

    public User {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(email, "email");
        roles = Set.copyOf(roles);
    }

    /**
     * The user as they are logged in.
     */
    public CurrentUser toCurrentUser() {
        return new CurrentUser(id, roles);
    }
}
