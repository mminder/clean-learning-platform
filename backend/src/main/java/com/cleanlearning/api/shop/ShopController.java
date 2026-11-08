package com.cleanlearning.api.shop;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cleanlearning.application.offering.GetShopOfferingUseCase;
import com.cleanlearning.application.offering.ListShopOfferingsUseCase;
import com.cleanlearning.domain.offering.OfferingId;

/**
 * The public shop (UC-02).
 */
@RestController
@RequestMapping("/api/shop")
class ShopController {

    private final ListShopOfferingsUseCase listShopOfferings;

    private final GetShopOfferingUseCase getShopOffering;

    ShopController(ListShopOfferingsUseCase listShopOfferings, GetShopOfferingUseCase getShopOffering) {
        this.listShopOfferings = listShopOfferings;
        this.getShopOffering = getShopOffering;
    }

    @GetMapping("/offerings")
    List<ShopOfferingResponse> listOfferings() {
        return listShopOfferings.invoke().stream().map(ShopOfferingResponse::from).toList();
    }

    @GetMapping("/offerings/{id}")
    ShopOfferingDetailResponse getOffering(@PathVariable UUID id) {
        return ShopOfferingDetailResponse.from(getShopOffering.invoke(new OfferingId(id)));
    }
}
