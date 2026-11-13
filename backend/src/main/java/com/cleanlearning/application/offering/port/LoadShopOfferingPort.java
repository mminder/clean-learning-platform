package com.cleanlearning.application.offering.port;

import com.cleanlearning.domain.offering.ShopConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;
import java.util.Optional;

public interface LoadShopOfferingPort {

  /**
   * The offering, if it's {@code PUBLISHED} and has at least one package, with its packages (fewest
   * sessions first) and its eligible experts (by display name).
   */
  Optional<ShopConsultationOffering> loadPublished(OfferingId id);
}
