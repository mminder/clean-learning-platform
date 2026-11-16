package com.cleanlearning.api.consultation.request;

import com.cleanlearning.application.consultation.BookConsultationAppointmentUseCase;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.user.value.UserId;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record BookConsultationAppointmentRequest(@NotNull UUID expertId, @NotNull Instant start) {

  public BookConsultationAppointmentUseCase.Command toCommand(
      ConsultationCaseId consultationCaseId) {
    return new BookConsultationAppointmentUseCase.Command(
        consultationCaseId, new UserId(expertId), start);
  }
}
