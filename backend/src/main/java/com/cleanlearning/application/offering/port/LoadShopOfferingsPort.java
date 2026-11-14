package com.cleanlearning.application.offering.port;

import com.cleanlearning.domain.offering.view.ShopOffering;
import java.util.List;

public interface LoadShopOfferingsPort {

  /** The {@code PUBLISHED} offerings that have at least one package, sorted by name. */
  List<ShopOffering> loadPublished();
}
