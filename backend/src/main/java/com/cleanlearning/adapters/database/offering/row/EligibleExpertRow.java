package com.cleanlearning.adapters.database.offering.row;

import java.util.UUID;

/** An eligible expert with the public fields of their expert profile. */
public record EligibleExpertRow(UUID id, String displayName, String bio) {}
