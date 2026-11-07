package com.cleanlearning.application.offering;

import java.util.Optional;

import com.cleanlearning.domain.offering.OfferingId;
import com.cleanlearning.domain.offering.ShopConsultationOffering;

public interface LoadShopOfferingPort {

    /**
     * The offering, if it's {@code PUBLISHED}, with its packages (fewest sessions first) and its eligible experts (by
     * display name).
     */
    Optional<ShopConsultationOffering> loadPublished(OfferingId id);
}
