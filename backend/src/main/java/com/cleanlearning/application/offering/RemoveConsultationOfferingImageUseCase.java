package com.cleanlearning.application.offering;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.application.image.ImageStorePort;
import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.NotFoundException;

/**
 * UC-16 step 3: the admin removes an offering's image, at once, apart from the form's save. The image is deleted, and
 * the shop shows the placeholder again. Without an image, nothing changes.
 */
@Service
public class RemoveConsultationOfferingImageUseCase {

    private final LockConsultationOfferingPort lockOffering;

    private final SaveConsultationOfferingPort saveOffering;

    private final ImageStorePort imageStore;

    RemoveConsultationOfferingImageUseCase(LockConsultationOfferingPort lockOffering,
            SaveConsultationOfferingPort saveOffering, ImageStorePort imageStore) {
        this.lockOffering = lockOffering;
        this.saveOffering = saveOffering;
        this.imageStore = imageStore;
    }

    /**
     * @throws NotFoundException if there's no consultation offering with this id
     * @throws BusinessRuleException if the offering is archived ({@code WRONG_OFFERING_STATUS})
     */
    @Transactional
    public ConsultationOffering invoke(OfferingId id) {
        ConsultationOffering offering = lockOffering.lock(id)
                .orElseThrow(() -> new NotFoundException("Offering " + id.value() + " not found."));
        ConsultationOffering changed = offering.removeImage();
        saveOffering.update(changed);
        offering.imageId().ifPresent(imageStore::delete);
        return changed;
    }
}
