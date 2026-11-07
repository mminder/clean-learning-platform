package com.cleanlearning.application.offering;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.domain.offering.ShopOffering;

/**
 * UC-02 steps 1–2: the published offerings for the shop, sorted by name.
 */
@Service
public class ListShopOfferingsService {

    private final LoadShopOfferingsPort loadShopOfferings;

    ListShopOfferingsService(LoadShopOfferingsPort loadShopOfferings) {
        this.loadShopOfferings = loadShopOfferings;
    }

    @Transactional(readOnly = true)
    public List<ShopOffering> list() {
        return loadShopOfferings.loadPublished();
    }
}
