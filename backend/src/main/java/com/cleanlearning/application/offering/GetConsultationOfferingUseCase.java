package com.cleanlearning.application.offering;

import com.cleanlearning.application.offering.port.LoadConsultationOfferingPort;
import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetConsultationOfferingUseCase {

  private final LoadConsultationOfferingPort loadOffering;

  GetConsultationOfferingUseCase(LoadConsultationOfferingPort loadOffering) {
    this.loadOffering = loadOffering;
  }

  @Transactional(readOnly = true)
  public ConsultationOffering invoke(OfferingId id) {
    return loadOffering
        .load(id)
        .orElseThrow(() -> new NotFoundException("Offering " + id.value() + " not found."));
  }
}
