package com.cleanlearning.adapters.database.offering;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cleanlearning.adapters.database.offering.row.ConsultationOfferingSummaryRow;
import com.cleanlearning.application.offering.LoadConsultationOfferingSummariesPort;
import com.cleanlearning.domain.offering.ConsultationOfferingSummary;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingName;

/**
 * The admin's side of the offerings (UC-16). The shop's side is {@code ShopOfferingAdapter}.
 */
@Component
class ConsultationOfferingAdapter implements LoadConsultationOfferingSummariesPort {

    private final ConsultationOfferingRepository repository;

    ConsultationOfferingAdapter(ConsultationOfferingRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ConsultationOfferingSummary> loadSummaries() {
        return repository.findSummaries().stream().map(ConsultationOfferingAdapter::toSummary).toList();
    }

    private static ConsultationOfferingSummary toSummary(ConsultationOfferingSummaryRow row) {
        return new ConsultationOfferingSummary(new OfferingId(row.id()), new OfferingName(row.name()), row.status(),
                Math.toIntExact(row.packageCount()), Math.toIntExact(row.eligibleExpertCount()));
    }
}
