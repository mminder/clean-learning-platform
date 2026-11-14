package com.cleanlearning.application.offering;

import com.cleanlearning.application.offering.port.LoadConsultationOfferingSummariesPort;
import com.cleanlearning.domain.offering.view.ConsultationOfferingSummary;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
