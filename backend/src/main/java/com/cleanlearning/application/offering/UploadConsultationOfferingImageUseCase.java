package com.cleanlearning.application.offering;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.application.image.port.ImageStorePort;
import com.cleanlearning.application.offering.port.LockConsultationOfferingPort;
import com.cleanlearning.application.offering.port.SaveConsultationOfferingPort;
import com.cleanlearning.domain.image.Image;
import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.NotFoundException;

/**
 * UC-16 step 3: the admin uploads an offering's image, or replaces it. It takes effect at once, apart from the form's
 * save. A replaced image is deleted, and the new one has a new id, so its URL can be cached for good (D-21).
 */
@Service
public class UploadConsultationOfferingImageUseCase {

    private final LockConsultationOfferingPort lockOffering;

    private final SaveConsultationOfferingPort saveOffering;

    private final ImageStorePort imageStore;

    UploadConsultationOfferingImageUseCase(LockConsultationOfferingPort lockOffering,
            SaveConsultationOfferingPort saveOffering, ImageStorePort imageStore) {
        this.lockOffering = lockOffering;
        this.saveOffering = saveOffering;
        this.imageStore = imageStore;
    }

    /**
     * @param file the uploaded file's bytes
     * @throws NotFoundException if there's no consultation offering with this id
     * @throws IllegalArgumentException if the file is empty, larger than 2 MB, or not a JPEG, PNG or WebP file
     * @throws BusinessRuleException if the offering is archived ({@code WRONG_OFFERING_STATUS})
     */
    @Transactional
    public ConsultationOffering invoke(OfferingId id, byte[] file) {
        ConsultationOffering offering = lockOffering.lock(id)
                .orElseThrow(() -> new NotFoundException("Offering " + id.value() + " not found."));
        Image image = Image.upload(file);
        ConsultationOffering changed = offering.changeImage(image.id());
        imageStore.save(image);
        saveOffering.update(changed);
        offering.imageId().ifPresent(imageStore::delete);
        return changed;
    }
}
