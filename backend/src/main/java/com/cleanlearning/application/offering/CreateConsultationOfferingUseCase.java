package com.cleanlearning.application.offering;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.domain.offering.ConsultationOffering;

/**
 * UC-16 steps 2–4: the admin creates a consultation offering. It starts as a draft.
 */
@Service
public class CreateConsultationOfferingUseCase {

    private final SaveConsultationOfferingPort saveOffering;

    CreateConsultationOfferingUseCase(SaveConsultationOfferingPort saveOffering) {
        this.saveOffering = saveOffering;
    }

    @Transactional
    public ConsultationOffering invoke(ConsultationOfferingCommand command) {
        ConsultationOffering offering = ConsultationOffering.create(command.name(), command.description(),
                command.sessionDuration(), command.cancellationDeadline());
        saveOffering.create(offering);
        return offering;
    }
}
