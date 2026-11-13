package com.cleanlearning.application.offering.port;

import com.cleanlearning.domain.offering.value.OfferingId;

public interface CheckOfferingPurchasesPort {

  /**
   * Whether a customer has bought a package of the offering: whether it has a case. The caller has
   * locked the offering ({@link LockConsultationOfferingPort}), and a purchase locks it too, so the
   * answer holds until the transaction ends (D-27).
   */
  boolean hasPurchases(OfferingId id);
}
