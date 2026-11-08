package com.cleanlearning.api.user.response;

import java.util.List;
import java.util.UUID;

import com.cleanlearning.domain.user.User;
import com.cleanlearning.domain.user.value.Role;

/**
 * The logged-in user, with their roles in the order {@code CUSTOMER}, {@code EXPERT}, {@code ADMIN}.
 */
public record MeResponse(UUID id, String name, String email, List<Role> roles) {

    public static MeResponse from(User user) {
        return new MeResponse(user.id().value(), user.name(), user.email(), user.roles().stream().sorted().toList());
    }
}
