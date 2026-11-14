package com.cleanlearning.application.offering.port;

import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.view.ShopConsultationOffering;
import java.util.Optional;

public interface LoadShopOfferingPort {

  Optional<ShopConsultationOffering> loadPublished(OfferingId id);
}
