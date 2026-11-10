package com.cleanlearning.application.consultation;

import java.time.Instant;
import java.util.Objects;

import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.user.value.UserId;

/**
 * What the customer confirmed in the booking dialog (UC-06 steps 2–6): the case, the expert the dialog showed (the one
 * they picked, or the case's), and the slot's start. The server books only with this expert (spec 04, decision 12).
 */
public record BookConsultationAppointmentCommand(ConsultationCaseId consultationCaseId, UserId expertId,
        Instant start) {

    public BookConsultationAppointmentCommand {
        Objects.requireNonNull(consultationCaseId, "consultationCaseId");
        Objects.requireNonNull(expertId, "expertId");
        Objects.requireNonNull(start, "start");
    }
}
