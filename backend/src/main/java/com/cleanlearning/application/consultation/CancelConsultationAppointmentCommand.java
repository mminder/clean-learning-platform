package com.cleanlearning.application.consultation;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import java.util.Objects;

public record CancelConsultationAppointmentCommand(
    ConsultationCaseId consultationCaseId, AppointmentId appointmentId, boolean creditRefunded) {

  public CancelConsultationAppointmentCommand {
    Objects.requireNonNull(consultationCaseId, "consultationCaseId");
    Objects.requireNonNull(appointmentId, "appointmentId");
  }
}
