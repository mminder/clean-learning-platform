package com.cleanlearning.adapters.database.offering.row;

import java.util.UUID;

import com.cleanlearning.domain.offering.value.OfferingStatus;

/**
 * One row per consultation offering, with its package and eligible expert counts.
 */
public record ConsultationOfferingSummaryRow(UUID id, String name, OfferingStatus status, long packageCount,
        long eligibleExpertCount) {
}
