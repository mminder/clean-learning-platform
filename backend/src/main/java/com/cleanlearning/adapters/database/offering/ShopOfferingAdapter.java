package com.cleanlearning.adapters.database.offering;

import com.cleanlearning.adapters.database.offering.row.ConsultationPackageRow;
import com.cleanlearning.adapters.database.offering.row.EligibleExpertRow;
import com.cleanlearning.adapters.database.offering.row.ShopConsultationOfferingRow;
import com.cleanlearning.adapters.database.offering.row.ShopOfferingPriceRow;
import com.cleanlearning.application.offering.port.LoadShopOfferingPort;
import com.cleanlearning.application.offering.port.LoadShopOfferingsPort;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.offering.ConsultationPackage;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.offering.view.ShopConsultationOffering;
import com.cleanlearning.domain.offering.view.ShopOffering;
import com.cleanlearning.domain.shared.value.Money;
import com.cleanlearning.domain.user.ExpertProfile;
import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.UserId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
class ShopOfferingAdapter implements LoadShopOfferingsPort, LoadShopOfferingPort {

  private final OfferingRepository offeringRepository;

  ShopOfferingAdapter(OfferingRepository offeringRepository) {
    this.offeringRepository = offeringRepository;
  }

  @Override
  public List<ShopOffering> loadPublished() {
    Map<UUID, List<ShopOfferingPriceRow>> rowsByOffering = new LinkedHashMap<>();
    for (ShopOfferingPriceRow row : offeringRepository.findPublishedWithPackagePrices()) {
      rowsByOffering.computeIfAbsent(row.id(), id -> new ArrayList<>()).add(row);
    }
    return rowsByOffering.values().stream().map(ShopOfferingAdapter::toShopOffering).toList();
  }

  @Override
  public Optional<ShopConsultationOffering> loadPublished(OfferingId id) {
    // Three queries, whatever the number of packages and experts: one joined query would repeat
    // every package for every expert.
    return offeringRepository
        .findPublishedConsultationOffering(id.value())
        .flatMap(this::loadWithPackages);
  }

  private Optional<ShopConsultationOffering> loadWithPackages(
      ShopConsultationOfferingRow offering) {
    List<ConsultationPackageRow> packages = offeringRepository.findPackages(offering.id());
    if (packages.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        toShopConsultationOffering(
            offering, packages, offeringRepository.findEligibleExperts(offering.id())));
  }

  private static ShopConsultationOffering toShopConsultationOffering(
      ShopConsultationOfferingRow offering,
      List<ConsultationPackageRow> packages,
      List<EligibleExpertRow> experts) {
    return new ShopConsultationOffering(
        new OfferingId(offering.id()),
        new OfferingName(offering.name()),
        offering.description(),
        imageId(offering.imageId()),
        new SessionDuration(offering.sessionDurationMinutes()),
        new CancellationDeadline(offering.cancellationDeadlineHours()),
        packages.stream().map(ShopOfferingAdapter::toConsultationPackage).toList(),
        experts.stream().map(ShopOfferingAdapter::toEligibleExpert).toList());
  }

  private static ConsultationPackage toConsultationPackage(ConsultationPackageRow row) {
    return new ConsultationPackage(
        new ConsultationPackageId(row.id()),
        new SessionCount(row.sessionCount()),
        new Money(row.price()));
  }

  private static ExpertProfile toEligibleExpert(EligibleExpertRow row) {
    return new ExpertProfile(new UserId(row.id()), new DisplayName(row.displayName()), row.bio());
  }

  private static ShopOffering toShopOffering(List<ShopOfferingPriceRow> rows) {
    ShopOfferingPriceRow first = rows.getFirst();
    List<Money> packagePrices = rows.stream().map(row -> new Money(row.price())).toList();
    return switch (first.type()) {
      case CONSULTATION ->
          ShopOffering.consultation(
              new OfferingId(first.id()),
              new OfferingName(first.name()),
              first.description(),
              imageId(first.imageId()),
              packagePrices);
    };
  }

  private static Optional<ImageId> imageId(@Nullable UUID imageId) {
    return Optional.ofNullable(imageId).map(ImageId::new);
  }
}
