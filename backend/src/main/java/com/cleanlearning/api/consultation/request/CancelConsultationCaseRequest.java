package com.cleanlearning.api.consultation.request;

import com.cleanlearning.application.consultation.CancelConsultationCaseUseCase;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import jakarta.validation.constraints.NotNull;

public record CancelConsultationCaseRequest(@NotNull Boolean refundEligible) {

  public CancelConsultationCaseUseCase.Command toCommand(ConsultationCaseId consultationCaseId) {
    return new CancelConsultationCaseUseCase.Command(consultationCaseId, refundEligible);
  }
}
