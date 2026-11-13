package com.cleanlearning.application.consultation;

import java.util.Objects;

import com.cleanlearning.domain.consultation.value.ConsultationCaseId;

/**
 * What the customer confirmed in the case's cancel dialog (UC-08 steps 1–3): the case, and whether the dialog said
 * the purchase price would be refunded. The server cancels only with this outcome (spec 05, decision 4).
 */
public record CancelConsultationCaseCommand(ConsultationCaseId consultationCaseId, boolean refundEligible) {

    public CancelConsultationCaseCommand {
        Objects.requireNonNull(consultationCaseId, "consultationCaseId");
    }
}
