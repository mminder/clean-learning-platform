package com.cleanlearning.application.offering;

import com.cleanlearning.application.offering.port.LockConsultationOfferingPort;
import com.cleanlearning.application.offering.port.SaveConsultationOfferingPort;
import com.cleanlearning.application.user.port.LoadExpertsPort;
import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EditConsultationOfferingUseCase {

  private final LockConsultationOfferingPort lockOffering;

  private final SaveConsultationOfferingPort saveOffering;

  private final LoadExpertsPort loadExperts;

  EditConsultationOfferingUseCase(
      LockConsultationOfferingPort lockOffering,
      SaveConsultationOfferingPort saveOffering,
      LoadExpertsPort loadExperts) {
    this.lockOffering = lockOffering;
    this.saveOffering = saveOffering;
    this.loadExperts = loadExperts;
  }

  @Transactional
  public ConsultationOffering invoke(OfferingId id, ConsultationOfferingCommand command) {
    ConsultationOffering offering =
        lockOffering
            .lock(id)
            .orElseThrow(() -> new NotFoundException("Offering " + id.value() + " not found."));
    ConsultationOffering edited =
        offering.edit(
            command.name(),
            command.description(),
            command.sessionDuration(),
            command.cancellationDeadline(),
            command.packages(),
            command.eligibleExpertIds(),
            loadExperts.loadExperts());
    saveOffering.update(edited);
    return edited;
  }
}
