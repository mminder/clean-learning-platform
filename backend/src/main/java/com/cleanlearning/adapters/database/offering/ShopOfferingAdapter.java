package com.cleanlearning.adapters.database.offering;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.cleanlearning.application.offering.LoadShopOfferingPort;
import com.cleanlearning.application.offering.LoadShopOfferingsPort;
import com.cleanlearning.domain.offering.CancellationDeadline;
import com.cleanlearning.domain.offering.ConsultationPackage;
import com.cleanlearning.domain.offering.ConsultationPackageId;
import com.cleanlearning.domain.offering.EligibleExpert;
import com.cleanlearning.domain.offering.OfferingId;
import com.cleanlearning.domain.offering.SessionDuration;
import com.cleanlearning.domain.offering.ShopConsultationOffering;
import com.cleanlearning.domain.offering.ShopOffering;
import com.cleanlearning.domain.shared.Money;
import com.cleanlearning.domain.user.UserId;

@Component
class ShopOfferingAdapter implements LoadShopOfferingsPort, LoadShopOfferingPort {

    private final OfferingRepository offeringRepository;

    ShopOfferingAdapter(OfferingRepository offeringRepository) {
        this.offeringRepository = offeringRepository;
    }

    @Override
    public List<ShopOffering> loadPublished() {
        // The rows come sorted by offering name; the map keeps that order and groups each offering's prices.
        Map<UUID, List<ShopOfferingPriceRow>> rowsByOffering = new LinkedHashMap<>();
        for (ShopOfferingPriceRow row : offeringRepository.findPublishedWithPackagePrices()) {
            rowsByOffering.computeIfAbsent(row.id(), id -> new ArrayList<>()).add(row);
        }
        return rowsByOffering.values().stream().map(ShopOfferingAdapter::toShopOffering).toList();
    }

    @Override
    public Optional<ShopConsultationOffering> loadPublished(OfferingId id) {
        // Three queries, whatever the number of packages and experts: one each for the offering, its packages and
        // its experts. One joined query would repeat every package for every expert.
        return offeringRepository.findPublishedConsultationOffering(id.value())
                .map(offering -> toShopConsultationOffering(offering,
                        offeringRepository.findPackages(offering.id()),
                        offeringRepository.findEligibleExperts(offering.id())));
    }

    private static ShopConsultationOffering toShopConsultationOffering(ShopConsultationOfferingRow offering,
            List<ConsultationPackageRow> packages, List<EligibleExpertRow> experts) {
        return new ShopConsultationOffering(new OfferingId(offering.id()), offering.name(), offering.description(),
                new SessionDuration(offering.sessionDurationMinutes()),
                new CancellationDeadline(offering.cancellationDeadlineHours()),
                packages.stream().map(ShopOfferingAdapter::toConsultationPackage).toList(),
                experts.stream().map(ShopOfferingAdapter::toEligibleExpert).toList());
    }

    private static ConsultationPackage toConsultationPackage(ConsultationPackageRow row) {
        return new ConsultationPackage(new ConsultationPackageId(row.id()), row.sessionCount(), new Money(row.price()));
    }

    private static EligibleExpert toEligibleExpert(EligibleExpertRow row) {
        return new EligibleExpert(new UserId(row.id()), row.displayName(), row.bio());
    }

    private static ShopOffering toShopOffering(List<ShopOfferingPriceRow> rows) {
        ShopOfferingPriceRow first = rows.getFirst();
        List<Money> packagePrices = rows.stream().map(row -> new Money(row.price())).toList();
        return switch (first.type()) {
            case CONSULTATION -> ShopOffering.consultation(new OfferingId(first.id()), first.name(),
                    first.description(), packagePrices);
        };
    }
}
