package com.cleanlearning.application.consultation.port;

import java.util.Optional;

import com.cleanlearning.domain.consultation.ConsultationOfferingForPurchase;
import com.cleanlearning.domain.offering.value.OfferingId;

public interface LockConsultationOfferingForPurchasePort {

    /**
     * Locks the offering's row until the transaction ends, waiting for every other lock of the row, and loads the
     * consultation offering as a purchase needs it, or returns empty if there's none with this id. The purchase calls
     * this first (D-27: offering → case → expert → run), so it and a change of the offering run one after the other.
     */
    Optional<ConsultationOfferingForPurchase> lock(OfferingId id);
}
