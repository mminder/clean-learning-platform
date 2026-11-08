package com.cleanlearning.application.offering;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.domain.offering.OfferingId;
import com.cleanlearning.domain.offering.ShopConsultationOffering;
import com.cleanlearning.domain.shared.NotFoundException;

/**
 * UC-02 steps 3–4: a published offering's detail page.
 */
@Service
public class GetShopOfferingService {

    private final LoadShopOfferingPort loadShopOffering;

    GetShopOfferingService(LoadShopOfferingPort loadShopOffering) {
        this.loadShopOffering = loadShopOffering;
    }

    /**
     * @throws NotFoundException if the offering doesn't exist, isn't {@code PUBLISHED} or has no package
     */
    @Transactional(readOnly = true)
    public ShopConsultationOffering get(OfferingId id) {
        return loadShopOffering.loadPublished(id)
                .orElseThrow(() -> new NotFoundException("Offering " + id.value() + " not found."));
    }
}
