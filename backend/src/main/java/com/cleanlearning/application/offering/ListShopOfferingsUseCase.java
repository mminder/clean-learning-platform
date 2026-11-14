package com.cleanlearning.application.offering;

import com.cleanlearning.application.offering.port.LoadShopOfferingsPort;
import com.cleanlearning.domain.offering.view.ShopOffering;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListShopOfferingsUseCase {

  private final LoadShopOfferingsPort loadShopOfferings;

  ListShopOfferingsUseCase(LoadShopOfferingsPort loadShopOfferings) {
    this.loadShopOfferings = loadShopOfferings;
  }

  @Transactional(readOnly = true)
  public List<ShopOffering> invoke() {
    return loadShopOfferings.loadPublished();
  }
}
