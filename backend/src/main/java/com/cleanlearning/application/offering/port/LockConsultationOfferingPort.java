package com.cleanlearning.application.offering.port;

import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;
import java.util.Optional;

public interface LockConsultationOfferingPort {

  /** Every use case that changes an offering calls this first (D-27). */
  Optional<ConsultationOffering> lock(OfferingId id);
}
