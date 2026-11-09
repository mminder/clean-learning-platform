package com.cleanlearning.application.offering;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.application.offering.port.LockConsultationOfferingPort;
import com.cleanlearning.application.offering.port.SaveConsultationOfferingPort;
import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.NotFoundException;

/**
 * UC-16 step 5: the admin archives a published offering. It leaves the shop for good and is read-only.
 */
@Service
public class ArchiveConsultationOfferingUseCase {

    private final LockConsultationOfferingPort lockOffering;

    private final SaveConsultationOfferingPort saveOffering;

    ArchiveConsultationOfferingUseCase(LockConsultationOfferingPort lockOffering,
            SaveConsultationOfferingPort saveOffering) {
        this.lockOffering = lockOffering;
        this.saveOffering = saveOffering;
    }

    /**
     * @throws NotFoundException if there's no consultation offering with this id
     * @throws BusinessRuleException if the offering isn't published ({@code WRONG_OFFERING_STATUS})
     */
    @Transactional
    public ConsultationOffering invoke(OfferingId id) {
        ConsultationOffering offering = lockOffering.lock(id)
                .orElseThrow(() -> new NotFoundException("Offering " + id.value() + " not found."));
        ConsultationOffering changed = offering.archive();
        saveOffering.update(changed);
        return changed;
    }
}
