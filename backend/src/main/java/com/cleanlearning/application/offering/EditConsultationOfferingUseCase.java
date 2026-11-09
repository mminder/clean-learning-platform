package com.cleanlearning.application.offering;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.application.offering.port.LockConsultationOfferingPort;
import com.cleanlearning.application.offering.port.SaveConsultationOfferingPort;
import com.cleanlearning.application.user.port.LoadExpertsPort;
import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.NotFoundException;

/**
 * UC-16 steps 3–4: the admin saves an existing consultation offering. It replaces the offering's fields, packages and
 * eligible experts; a published offering's changes show in the shop at once.
 */
@Service
public class EditConsultationOfferingUseCase {

    private final LockConsultationOfferingPort lockOffering;

    private final SaveConsultationOfferingPort saveOffering;

    private final LoadExpertsPort loadExperts;

    EditConsultationOfferingUseCase(LockConsultationOfferingPort lockOffering,
            SaveConsultationOfferingPort saveOffering, LoadExpertsPort loadExperts) {
        this.lockOffering = lockOffering;
        this.saveOffering = saveOffering;
        this.loadExperts = loadExperts;
    }

    /**
     * @throws NotFoundException if there's no consultation offering with this id
     * @throws IllegalArgumentException if a package id isn't one of this offering's, a package is named twice, or
     *     an eligible expert id isn't an expert's
     * @throws BusinessRuleException if the offering is archived ({@code WRONG_OFFERING_STATUS})
     */
    @Transactional
    public ConsultationOffering invoke(OfferingId id, ConsultationOfferingCommand command) {
        ConsultationOffering offering = lockOffering.lock(id)
                .orElseThrow(() -> new NotFoundException("Offering " + id.value() + " not found."));
        ConsultationOffering edited = offering.edit(command.name(), command.description(), command.sessionDuration(),
                command.cancellationDeadline(), command.packages(), command.eligibleExpertIds(),
                loadExperts.loadExperts());
        saveOffering.update(edited);
        return edited;
    }
}
