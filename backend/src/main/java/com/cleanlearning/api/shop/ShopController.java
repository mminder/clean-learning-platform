package com.cleanlearning.api.shop;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cleanlearning.application.offering.ListShopOfferingsService;

/**
 * The public shop (UC-02).
 */
@RestController
@RequestMapping("/api/shop")
class ShopController {

    private final ListShopOfferingsService listShopOfferings;

    ShopController(ListShopOfferingsService listShopOfferings) {
        this.listShopOfferings = listShopOfferings;
    }

    @GetMapping("/offerings")
    List<ShopOfferingResponse> listOfferings() {
        return listShopOfferings.list().stream().map(ShopOfferingResponse::from).toList();
    }
}
