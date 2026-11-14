package com.cleanlearning.application.offering.port;

import com.cleanlearning.domain.offering.value.OfferingId;

public interface DeleteConsultationOfferingPort {

  void delete(OfferingId id);
}
