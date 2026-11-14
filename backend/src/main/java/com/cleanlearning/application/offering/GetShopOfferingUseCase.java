package com.cleanlearning.application.offering;

import com.cleanlearning.application.offering.port.LoadShopOfferingPort;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.view.ShopConsultationOffering;
import com.cleanlearning.domain.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetShopOfferingUseCase {

  private final LoadShopOfferingPort loadShopOffering;

  GetShopOfferingUseCase(LoadShopOfferingPort loadShopOffering) {
    this.loadShopOffering = loadShopOffering;
  }

  @Transactional(readOnly = true)
  public ShopConsultationOffering invoke(OfferingId id) {
    return loadShopOffering
        .loadPublished(id)
        .orElseThrow(() -> new NotFoundException("Offering " + id.value() + " not found."));
  }
}
