package com.cleanlearning.application.offering.port;

import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;
import java.util.Optional;

public interface LockConsultationOfferingPort {

  /**
   * Locks the offering's row until the transaction ends, waiting for every other lock of the row,
   * and loads the consultation offering, or returns empty if there's none with this id. Every use
   * case that changes an offering calls this first (D-27: offering → case → expert → run).
   */
  Optional<ConsultationOffering> lock(OfferingId id);
}
