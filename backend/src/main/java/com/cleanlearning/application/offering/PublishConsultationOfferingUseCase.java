package com.cleanlearning.application.offering;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.NotFoundException;

/**
 * UC-16 step 5: the admin publishes a draft. It appears in the shop.
 */
@Service
public class PublishConsultationOfferingUseCase {

    private final LockConsultationOfferingPort lockOffering;

    private final SaveConsultationOfferingPort saveOffering;

    PublishConsultationOfferingUseCase(LockConsultationOfferingPort lockOffering,
            SaveConsultationOfferingPort saveOffering) {
        this.lockOffering = lockOffering;
        this.saveOffering = saveOffering;
    }

    /**
     * @throws NotFoundException if there's no consultation offering with this id
     * @throws BusinessRuleException if the offering isn't a draft ({@code WRONG_OFFERING_STATUS}), or has no package or no eligible expert ({@code OFFERING_INCOMPLETE})
     */
    @Transactional
    public ConsultationOffering invoke(OfferingId id) {
        ConsultationOffering offering = lockOffering.lock(id)
                .orElseThrow(() -> new NotFoundException("Offering " + id.value() + " not found."));
        ConsultationOffering changed = offering.publish();
        saveOffering.update(changed);
        return changed;
    }
}
