package com.cleanlearning.application.offering;

import com.cleanlearning.application.image.port.ImageStorePort;
import com.cleanlearning.application.offering.port.LockConsultationOfferingPort;
import com.cleanlearning.application.offering.port.SaveConsultationOfferingPort;
import com.cleanlearning.domain.image.Image;
import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A replaced image is deleted, and the new one has a new id, so its URL can be cached for good
 * (D-21).
 */
@Service
public class UploadConsultationOfferingImageUseCase {

  private final LockConsultationOfferingPort lockOffering;

  private final SaveConsultationOfferingPort saveOffering;

  private final ImageStorePort imageStore;

  UploadConsultationOfferingImageUseCase(
      LockConsultationOfferingPort lockOffering,
      SaveConsultationOfferingPort saveOffering,
      ImageStorePort imageStore) {
    this.lockOffering = lockOffering;
    this.saveOffering = saveOffering;
    this.imageStore = imageStore;
  }

  @Transactional
  public ConsultationOffering invoke(OfferingId id, byte[] file) {
    ConsultationOffering offering =
        lockOffering
            .lock(id)
            .orElseThrow(() -> new NotFoundException("Offering " + id.value() + " not found."));
    Image image = Image.upload(file);
    ConsultationOffering changed = offering.changeImage(image.id());
    imageStore.save(image);
    saveOffering.update(changed);
    offering.imageId().ifPresent(imageStore::delete);
    return changed;
  }
}
