package com.cleanlearning.adapters.database.offering;

import java.util.UUID;

/**
 * An eligible expert with the public fields of their expert profile.
 */
record EligibleExpertRow(UUID id, String displayName, String bio) {
}
