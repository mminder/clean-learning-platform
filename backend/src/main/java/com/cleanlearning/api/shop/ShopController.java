package com.cleanlearning.api.shop;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cleanlearning.application.offering.GetShopOfferingService;
import com.cleanlearning.application.offering.ListShopOfferingsService;
import com.cleanlearning.domain.offering.OfferingId;

/**
 * The public shop (UC-02).
 */
@RestController
@RequestMapping("/api/shop")
class ShopController {

    private final ListShopOfferingsService listShopOfferings;

    private final GetShopOfferingService getShopOffering;

    ShopController(ListShopOfferingsService listShopOfferings, GetShopOfferingService getShopOffering) {
        this.listShopOfferings = listShopOfferings;
        this.getShopOffering = getShopOffering;
    }

    @GetMapping("/offerings")
    List<ShopOfferingResponse> listOfferings() {
        return listShopOfferings.list().stream().map(ShopOfferingResponse::from).toList();
    }

    @GetMapping("/offerings/{id}")
    ShopOfferingDetailResponse getOffering(@PathVariable UUID id) {
        return ShopOfferingDetailResponse.from(getShopOffering.get(new OfferingId(id)));
    }
}
