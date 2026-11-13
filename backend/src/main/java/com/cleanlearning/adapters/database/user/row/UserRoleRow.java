package com.cleanlearning.adapters.database.user.row;

import com.cleanlearning.domain.user.value.Role;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A user with one of their roles: a user has a row per role, and one row without a role if they
 * have none.
 */
public record UserRoleRow(UUID id, String name, String email, @Nullable Role role) {}
