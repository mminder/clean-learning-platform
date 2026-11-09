package com.cleanlearning.application.offering;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.application.offering.port.LoadConsultationOfferingSummariesPort;
import com.cleanlearning.domain.offering.ConsultationOfferingSummary;

/**
 * UC-16 step 1: every consultation offering in every status, sorted by name.
 */
@Service
public class ListConsultationOfferingsUseCase {

    private final LoadConsultationOfferingSummariesPort loadSummaries;

    ListConsultationOfferingsUseCase(LoadConsultationOfferingSummariesPort loadSummaries) {
        this.loadSummaries = loadSummaries;
    }

    @Transactional(readOnly = true)
    public List<ConsultationOfferingSummary> invoke() {
        return loadSummaries.loadSummaries();
    }
}
