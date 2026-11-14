package com.cleanlearning.adapters.database.user.row;

import com.cleanlearning.domain.user.value.Role;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** One row per role, and one row without a role if the user has none. */
public record UserRoleRow(UUID id, String name, String email, @Nullable Role role) {}
