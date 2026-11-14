package com.cleanlearning.application.offering;

import com.cleanlearning.application.image.port.ImageStorePort;
import com.cleanlearning.application.offering.port.CheckOfferingPurchasesPort;
import com.cleanlearning.application.offering.port.DeleteConsultationOfferingPort;
import com.cleanlearning.application.offering.port.LockConsultationOfferingPort;
import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteConsultationOfferingUseCase {

  private final LockConsultationOfferingPort lockOffering;

  private final CheckOfferingPurchasesPort checkPurchases;

  private final DeleteConsultationOfferingPort deleteOffering;

  private final ImageStorePort imageStore;

  DeleteConsultationOfferingUseCase(
      LockConsultationOfferingPort lockOffering,
      CheckOfferingPurchasesPort checkPurchases,
      DeleteConsultationOfferingPort deleteOffering,
      ImageStorePort imageStore) {
    this.lockOffering = lockOffering;
    this.checkPurchases = checkPurchases;
    this.deleteOffering = deleteOffering;
    this.imageStore = imageStore;
  }

  /** Asks for purchases under the offering's lock, which a purchase takes too (D-27). */
  @Transactional
  public void invoke(OfferingId id) {
    ConsultationOffering offering =
        lockOffering
            .lock(id)
            .orElseThrow(() -> new NotFoundException("Offering " + id.value() + " not found."));
    offering.requireDeletable(checkPurchases.hasPurchases(id));
    deleteOffering.delete(id);
    // Deleted after the offering, which refers to it.
    offering.imageId().ifPresent(imageStore::delete);
  }
}
