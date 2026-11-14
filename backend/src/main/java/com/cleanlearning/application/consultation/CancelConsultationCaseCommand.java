package com.cleanlearning.application.consultation;

import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import java.util.Objects;

public record CancelConsultationCaseCommand(
    ConsultationCaseId consultationCaseId, boolean refundEligible) {

  public CancelConsultationCaseCommand {
    Objects.requireNonNull(consultationCaseId, "consultationCaseId");
  }
}
