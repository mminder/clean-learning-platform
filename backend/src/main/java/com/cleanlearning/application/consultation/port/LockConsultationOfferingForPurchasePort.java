package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.ConsultationOfferingForPurchase;
import com.cleanlearning.domain.offering.value.OfferingId;
import java.util.Optional;

public interface LockConsultationOfferingForPurchasePort {

  /**
   * Called first by the purchase (D-27), so it and a change of the offering run one after the
   * other.
   */
  Optional<ConsultationOfferingForPurchase> lock(OfferingId id);
}
