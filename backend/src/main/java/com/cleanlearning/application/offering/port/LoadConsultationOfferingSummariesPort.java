package com.cleanlearning.application.offering.port;

import com.cleanlearning.domain.offering.view.ConsultationOfferingSummary;
import java.util.List;

public interface LoadConsultationOfferingSummariesPort {

  List<ConsultationOfferingSummary> loadSummaries();
}
