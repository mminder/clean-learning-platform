package com.cleanlearning.adapters.database.user.row;

import java.util.UUID;

/** An expert: a user with {@code EXPERT} and their expert profile's display name. */
public record ExpertRow(UUID id, String displayName) {}
