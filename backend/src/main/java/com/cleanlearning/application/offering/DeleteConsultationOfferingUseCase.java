package com.cleanlearning.application.offering;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.application.image.ImageStorePort;
import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.NotFoundException;

/**
 * UC-16 step 5: the admin deletes a draft for good, with its packages, eligible experts and image.
 */
@Service
public class DeleteConsultationOfferingUseCase {

    private final LockConsultationOfferingPort lockOffering;

    private final DeleteConsultationOfferingPort deleteOffering;

    private final ImageStorePort imageStore;

    DeleteConsultationOfferingUseCase(LockConsultationOfferingPort lockOffering,
            DeleteConsultationOfferingPort deleteOffering, ImageStorePort imageStore) {
        this.lockOffering = lockOffering;
        this.deleteOffering = deleteOffering;
        this.imageStore = imageStore;
    }

    /**
     * @throws NotFoundException if there's no consultation offering with this id
     * @throws BusinessRuleException if the offering isn't a draft ({@code WRONG_OFFERING_STATUS})
     */
    @Transactional
    public void invoke(OfferingId id) {
        ConsultationOffering offering = lockOffering.lock(id)
                .orElseThrow(() -> new NotFoundException("Offering " + id.value() + " not found."));
        offering.requireDeletable();
        deleteOffering.delete(id);
        // after the offering, which refers to it
        offering.imageId().ifPresent(imageStore::delete);
    }
}
