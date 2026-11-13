package com.cleanlearning.application.offering.port;

import com.cleanlearning.domain.offering.value.OfferingId;

public interface DeleteConsultationOfferingPort {

  /**
   * Deletes the consultation offering for good, with its packages and eligible experts. The caller
   * has locked it ({@link LockConsultationOfferingPort}).
   */
  void delete(OfferingId id);
}
