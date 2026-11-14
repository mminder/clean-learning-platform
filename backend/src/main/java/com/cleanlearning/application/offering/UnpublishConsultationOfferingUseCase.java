package com.cleanlearning.application.offering;

import com.cleanlearning.application.offering.port.CheckOfferingPurchasesPort;
import com.cleanlearning.application.offering.port.LockConsultationOfferingPort;
import com.cleanlearning.application.offering.port.SaveConsultationOfferingPort;
import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnpublishConsultationOfferingUseCase {

  private final LockConsultationOfferingPort lockOffering;

  private final CheckOfferingPurchasesPort checkPurchases;

  private final SaveConsultationOfferingPort saveOffering;

  UnpublishConsultationOfferingUseCase(
      LockConsultationOfferingPort lockOffering,
      CheckOfferingPurchasesPort checkPurchases,
      SaveConsultationOfferingPort saveOffering) {
    this.lockOffering = lockOffering;
    this.checkPurchases = checkPurchases;
    this.saveOffering = saveOffering;
  }

  /**
   * Asks for purchases under the offering's lock, which a purchase takes too (D-27): a purchase
   * either committed before, and counts, or waits and then finds a draft.
   */
  @Transactional
  public ConsultationOffering invoke(OfferingId id) {
    ConsultationOffering offering =
        lockOffering
            .lock(id)
            .orElseThrow(() -> new NotFoundException("Offering " + id.value() + " not found."));
    ConsultationOffering changed = offering.unpublish(checkPurchases.hasPurchases(id));
    saveOffering.update(changed);
    return changed;
  }
}
