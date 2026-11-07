package com.cleanlearning.application.offering;

import java.util.List;

import com.cleanlearning.domain.offering.ShopOffering;

public interface LoadShopOfferingsPort {

    /**
     * The {@code PUBLISHED} offerings, sorted by name.
     */
    List<ShopOffering> loadPublished();
}
