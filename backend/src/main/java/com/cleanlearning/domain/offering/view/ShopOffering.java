package com.cleanlearning.domain.offering.view;

import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.OfferingType;
import com.cleanlearning.domain.shared.value.Money;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** The price is the cheapest package's, shown as "from". */
public record ShopOffering(
    OfferingId id,
    OfferingType type,
    OfferingName name,
    String description,
    Optional<ImageId> imageId,
    Money price) {

  public ShopOffering {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(imageId, "imageId");
    Objects.requireNonNull(price, "price");
  }

  public static ShopOffering consultation(
      OfferingId id,
      OfferingName name,
      String description,
      Optional<ImageId> imageId,
      List<Money> packagePrices) {
    if (packagePrices.isEmpty()) {
      throw new IllegalArgumentException(
          "A consultation offering needs at least one package: " + id);
    }
    return new ShopOffering(
        id, OfferingType.CONSULTATION, name, description, imageId, Collections.min(packagePrices));
  }
}
