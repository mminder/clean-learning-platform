package com.cleanlearning.application.offering;

import com.cleanlearning.application.offering.port.SaveConsultationOfferingPort;
import com.cleanlearning.application.user.port.LoadExpertsPort;
import com.cleanlearning.domain.offering.ConsultationOffering;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateConsultationOfferingUseCase {

  private final SaveConsultationOfferingPort saveOffering;

  private final LoadExpertsPort loadExperts;

  CreateConsultationOfferingUseCase(
      SaveConsultationOfferingPort saveOffering, LoadExpertsPort loadExperts) {
    this.saveOffering = saveOffering;
    this.loadExperts = loadExperts;
  }

  @Transactional
  public ConsultationOffering invoke(ConsultationOfferingCommand command) {
    ConsultationOffering offering =
        ConsultationOffering.create(
            command.name(),
            command.description(),
            command.sessionDuration(),
            command.cancellationDeadline(),
            command.packages(),
            command.eligibleExpertIds(),
            loadExperts.loadExperts());
    saveOffering.create(offering);
    return offering;
  }
}
