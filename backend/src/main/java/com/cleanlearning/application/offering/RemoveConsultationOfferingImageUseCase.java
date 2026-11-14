package com.cleanlearning.application.offering;

import com.cleanlearning.application.image.port.ImageStorePort;
import com.cleanlearning.application.offering.port.LockConsultationOfferingPort;
import com.cleanlearning.application.offering.port.SaveConsultationOfferingPort;
import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RemoveConsultationOfferingImageUseCase {

  private final LockConsultationOfferingPort lockOffering;

  private final SaveConsultationOfferingPort saveOffering;

  private final ImageStorePort imageStore;

  RemoveConsultationOfferingImageUseCase(
      LockConsultationOfferingPort lockOffering,
      SaveConsultationOfferingPort saveOffering,
      ImageStorePort imageStore) {
    this.lockOffering = lockOffering;
    this.saveOffering = saveOffering;
    this.imageStore = imageStore;
  }

  @Transactional
  public ConsultationOffering invoke(OfferingId id) {
    ConsultationOffering offering =
        lockOffering
            .lock(id)
            .orElseThrow(() -> new NotFoundException("Offering " + id.value() + " not found."));
    ConsultationOffering changed = offering.removeImage();
    saveOffering.update(changed);
    offering.imageId().ifPresent(imageStore::delete);
    return changed;
  }
}
