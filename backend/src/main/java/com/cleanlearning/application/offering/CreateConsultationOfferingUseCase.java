package com.cleanlearning.application.offering;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.application.offering.port.SaveConsultationOfferingPort;
import com.cleanlearning.application.user.port.LoadExpertsPort;
import com.cleanlearning.domain.offering.ConsultationOffering;

/**
 * UC-16 steps 2–4: the admin creates a consultation offering. It starts as a draft.
 */
@Service
public class CreateConsultationOfferingUseCase {

    private final SaveConsultationOfferingPort saveOffering;

    private final LoadExpertsPort loadExperts;

    CreateConsultationOfferingUseCase(SaveConsultationOfferingPort saveOffering, LoadExpertsPort loadExperts) {
        this.saveOffering = saveOffering;
        this.loadExperts = loadExperts;
    }

    /**
     * @throws IllegalArgumentException if a package names an id, or an eligible expert id isn't an expert's
     */
    @Transactional
    public ConsultationOffering invoke(ConsultationOfferingCommand command) {
        ConsultationOffering offering = ConsultationOffering.create(command.name(), command.description(),
                command.sessionDuration(), command.cancellationDeadline(), command.packages(),
                command.eligibleExpertIds(), loadExperts.loadExperts());
        saveOffering.create(offering);
        return offering;
    }
}
