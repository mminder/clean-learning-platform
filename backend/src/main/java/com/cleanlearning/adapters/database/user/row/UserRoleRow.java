package com.cleanlearning.adapters.database.user.row;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.cleanlearning.domain.user.value.Role;

/**
 * A user with one of their roles: a user has a row per role, and one row without a role if they have none.
 */
public record UserRoleRow(UUID id, String name, String email, @Nullable Role role) {
}
