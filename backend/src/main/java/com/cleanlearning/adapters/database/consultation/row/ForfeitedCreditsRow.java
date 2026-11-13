package com.cleanlearning.adapters.database.consultation.row;

import java.util.UUID;

/** A case's forfeited credits: the number of its appointments cancelled late. Cases without one have no row. */
public record ForfeitedCreditsRow(UUID consultationCaseId, long count) {
}
