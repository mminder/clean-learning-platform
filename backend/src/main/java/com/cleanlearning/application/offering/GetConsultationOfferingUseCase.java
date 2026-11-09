package com.cleanlearning.application.offering;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.application.offering.port.LoadConsultationOfferingPort;
import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.shared.NotFoundException;

/**
 * UC-16 step 2: the admin opens a consultation offering, in any status.
 */
@Service
public class GetConsultationOfferingUseCase {

    private final LoadConsultationOfferingPort loadOffering;

    GetConsultationOfferingUseCase(LoadConsultationOfferingPort loadOffering) {
        this.loadOffering = loadOffering;
    }

    /**
     * @throws NotFoundException if there's no consultation offering with this id
     */
    @Transactional(readOnly = true)
    public ConsultationOffering invoke(OfferingId id) {
        return loadOffering.load(id).orElseThrow(() -> new NotFoundException("Offering " + id.value() + " not found."));
    }
}
