package com.cleanlearning.application.offering.port;

import java.util.List;

import com.cleanlearning.domain.offering.ConsultationOfferingSummary;

public interface LoadConsultationOfferingSummariesPort {

    /**
     * Every consultation offering in every status, sorted by name, then id.
     */
    List<ConsultationOfferingSummary> loadSummaries();
}
