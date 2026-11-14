package com.cleanlearning.application.offering.port;

import com.cleanlearning.domain.offering.view.ShopOffering;
import java.util.List;

public interface LoadShopOfferingsPort {

  List<ShopOffering> loadPublished();
}
