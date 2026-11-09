package com.cleanlearning.api.user.response;

import java.util.List;
import java.util.UUID;

import com.cleanlearning.domain.user.User;
import com.cleanlearning.domain.user.value.Role;

/**
 * A user on the PoC login page, with their roles in the order {@code CUSTOMER}, {@code EXPERT}, {@code ADMIN}.
 */
public record PocUserResponse(UUID id, String name, List<Role> roles) {

    public static PocUserResponse from(User user) {
        return new PocUserResponse(user.id().value(), user.name(), user.roles().stream().sorted().toList());
    }
}
