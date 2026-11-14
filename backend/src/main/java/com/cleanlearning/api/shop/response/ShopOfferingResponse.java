package com.cleanlearning.api.shop.response;

import com.cleanlearning.api.response.MoneyResponse;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.offering.value.OfferingType;
import com.cleanlearning.domain.offering.view.ShopOffering;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * An offering card in the shop. For a consultation offering, the price is its cheapest package
 * ("from"). The image id is {@code null} without an image, and the card shows a placeholder.
 */
public record ShopOfferingResponse(
    UUID id,
    OfferingType type,
    String name,
    String description,
    @Nullable UUID imageId,
    MoneyResponse price) {

  public static ShopOfferingResponse from(ShopOffering offering) {
    return new ShopOfferingResponse(
        offering.id().value(),
        offering.type(),
        offering.name().value(),
        offering.description(),
        offering.imageId().map(ImageId::value).orElse(null),
        MoneyResponse.from(offering.price()));
  }
}
