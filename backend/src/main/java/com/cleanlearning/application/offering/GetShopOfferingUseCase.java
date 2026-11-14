package com.cleanlearning.application.offering;

import com.cleanlearning.application.offering.port.LoadShopOfferingPort;
import com.cleanlearning.domain.offering.ShopConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC-02 steps 3–4: a published offering's detail page. */
@Service
public class GetShopOfferingUseCase {

  private final LoadShopOfferingPort loadShopOffering;

  GetShopOfferingUseCase(LoadShopOfferingPort loadShopOffering) {
    this.loadShopOffering = loadShopOffering;
  }

  /**
   * @throws NotFoundException if the offering doesn't exist, isn't {@code PUBLISHED} or has no
   *     package
   */
  @Transactional(readOnly = true)
  public ShopConsultationOffering invoke(OfferingId id) {
    return loadShopOffering
        .loadPublished(id)
        .orElseThrow(() -> new NotFoundException("Offering " + id.value() + " not found."));
  }
}
