package com.cleanlearning.application.offering.port;

import com.cleanlearning.domain.offering.value.OfferingId;

public interface CheckOfferingPurchasesPort {

  /** The caller has locked the offering, so the answer holds until the transaction ends (D-27). */
  boolean hasPurchases(OfferingId id);
}
