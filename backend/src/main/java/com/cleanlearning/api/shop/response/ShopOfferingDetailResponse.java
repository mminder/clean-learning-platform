package com.cleanlearning.api.shop.response;

import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.offering.value.OfferingType;
import com.cleanlearning.domain.offering.view.ShopConsultationOffering;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * An offering's detail page in the shop. Trainings will add a {@code training} part next to {@code
 * consultation}. The image id is {@code null} without an image, and the page shows a placeholder.
 */
public record ShopOfferingDetailResponse(
    UUID id,
    OfferingType type,
    String name,
    String description,
    @Nullable UUID imageId,
    ConsultationOfferingDetailResponse consultation) {

  public static ShopOfferingDetailResponse from(ShopConsultationOffering offering) {
    return new ShopOfferingDetailResponse(
        offering.id().value(),
        offering.type(),
        offering.name().value(),
        offering.description(),
        offering.imageId().map(ImageId::value).orElse(null),
        ConsultationOfferingDetailResponse.from(offering));
  }
}
