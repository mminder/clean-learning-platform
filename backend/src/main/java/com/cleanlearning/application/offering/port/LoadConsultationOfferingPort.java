package com.cleanlearning.application.offering.port;

import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;
import java.util.Optional;

public interface LoadConsultationOfferingPort {

  Optional<ConsultationOffering> load(OfferingId id);
}
