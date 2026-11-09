package com.cleanlearning.application.offering;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.application.offering.port.CheckOfferingPurchasesPort;
import com.cleanlearning.application.offering.port.LockConsultationOfferingPort;
import com.cleanlearning.application.offering.port.SaveConsultationOfferingPort;
import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.NotFoundException;

/**
 * UC-16 step 5: the admin unpublishes a published offering without purchases. It leaves the shop and is a draft
 * again.
 */
@Service
public class UnpublishConsultationOfferingUseCase {

    private final LockConsultationOfferingPort lockOffering;

    private final CheckOfferingPurchasesPort checkPurchases;

    private final SaveConsultationOfferingPort saveOffering;

    UnpublishConsultationOfferingUseCase(LockConsultationOfferingPort lockOffering,
            CheckOfferingPurchasesPort checkPurchases, SaveConsultationOfferingPort saveOffering) {
        this.lockOffering = lockOffering;
        this.checkPurchases = checkPurchases;
        this.saveOffering = saveOffering;
    }

    /**
     * Asks for purchases under the offering's lock, which a purchase takes too (D-27): a purchase either committed
     * before, and counts, or waits and then finds a draft.
     *
     * @throws NotFoundException if there's no consultation offering with this id
     * @throws BusinessRuleException if the offering isn't published ({@code WRONG_OFFERING_STATUS}), or has purchases
     *     ({@code OFFERING_HAS_PURCHASES})
     */
    @Transactional
    public ConsultationOffering invoke(OfferingId id) {
        ConsultationOffering offering = lockOffering.lock(id)
                .orElseThrow(() -> new NotFoundException("Offering " + id.value() + " not found."));
        ConsultationOffering changed = offering.unpublish(checkPurchases.hasPurchases(id));
        saveOffering.update(changed);
        return changed;
    }
}
