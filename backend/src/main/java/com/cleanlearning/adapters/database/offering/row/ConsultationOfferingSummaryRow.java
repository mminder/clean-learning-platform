package com.cleanlearning.adapters.database.offering.row;

import com.cleanlearning.domain.offering.value.OfferingStatus;
import java.util.UUID;

public record ConsultationOfferingSummaryRow(
    UUID id, String name, OfferingStatus status, long packageCount, long eligibleExpertCount) {}
