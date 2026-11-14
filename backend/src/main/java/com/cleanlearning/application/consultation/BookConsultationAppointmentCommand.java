package com.cleanlearning.application.consultation;

import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.user.value.UserId;
import java.time.Instant;
import java.util.Objects;

public record BookConsultationAppointmentCommand(
    ConsultationCaseId consultationCaseId, UserId expertId, Instant start) {

  public BookConsultationAppointmentCommand {
    Objects.requireNonNull(consultationCaseId, "consultationCaseId");
    Objects.requireNonNull(expertId, "expertId");
    Objects.requireNonNull(start, "start");
  }
}
