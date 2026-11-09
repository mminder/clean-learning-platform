package com.cleanlearning.application.offering.port;

import java.util.Optional;

import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;

public interface LoadConsultationOfferingPort {

    /**
     * The consultation offering in any status, or empty if there's none with this id.
     */
    Optional<ConsultationOffering> load(OfferingId id);
}
