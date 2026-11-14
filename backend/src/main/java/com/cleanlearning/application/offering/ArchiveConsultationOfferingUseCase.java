package com.cleanlearning.application.offering;

import com.cleanlearning.application.offering.port.LockConsultationOfferingPort;
import com.cleanlearning.application.offering.port.SaveConsultationOfferingPort;
import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArchiveConsultationOfferingUseCase {

  private final LockConsultationOfferingPort lockOffering;

  private final SaveConsultationOfferingPort saveOffering;

  ArchiveConsultationOfferingUseCase(
      LockConsultationOfferingPort lockOffering, SaveConsultationOfferingPort saveOffering) {
    this.lockOffering = lockOffering;
    this.saveOffering = saveOffering;
  }

  @Transactional
  public ConsultationOffering invoke(OfferingId id) {
    ConsultationOffering offering =
        lockOffering
            .lock(id)
            .orElseThrow(() -> new NotFoundException("Offering " + id.value() + " not found."));
    ConsultationOffering changed = offering.archive();
    saveOffering.update(changed);
    return changed;
  }
}
