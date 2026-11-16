package com.cleanlearning.api.consultation.request;

import com.cleanlearning.application.consultation.CancelConsultationAppointmentUseCase;
import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import jakarta.validation.constraints.NotNull;

public record CancelConsultationAppointmentRequest(@NotNull Boolean creditRefunded) {

  public CancelConsultationAppointmentUseCase.Command toCommand(
      ConsultationCaseId consultationCaseId, AppointmentId appointmentId) {
    return new CancelConsultationAppointmentUseCase.Command(
        consultationCaseId, appointmentId, creditRefunded);
  }
}
