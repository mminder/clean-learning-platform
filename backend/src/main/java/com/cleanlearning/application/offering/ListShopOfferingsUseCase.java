package com.cleanlearning.application.offering;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.application.offering.port.LoadShopOfferingsPort;
import com.cleanlearning.domain.offering.ShopOffering;

/**
 * UC-02 steps 1–2: the published offerings for the shop, sorted by name.
 */
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
