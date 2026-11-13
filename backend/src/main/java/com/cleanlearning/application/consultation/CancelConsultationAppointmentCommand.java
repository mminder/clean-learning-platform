package com.cleanlearning.application.consultation;

import java.util.Objects;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;

/**
 * What the customer confirmed in the cancel dialog (UC-07 steps 1–3): the case, its appointment, and whether the
 * dialog said the credit would be returned. The server cancels only with this outcome (spec 05, decision 4).
 */
public record CancelConsultationAppointmentCommand(ConsultationCaseId consultationCaseId, AppointmentId appointmentId,
        boolean creditRefunded) {

    public CancelConsultationAppointmentCommand {
        Objects.requireNonNull(consultationCaseId, "consultationCaseId");
        Objects.requireNonNull(appointmentId, "appointmentId");
    }
}
