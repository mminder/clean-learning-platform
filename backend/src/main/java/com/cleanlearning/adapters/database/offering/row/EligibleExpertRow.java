package com.cleanlearning.adapters.database.offering.row;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record EligibleExpertRow(UUID id, String displayName, String bio, @Nullable UUID photoId) {}
