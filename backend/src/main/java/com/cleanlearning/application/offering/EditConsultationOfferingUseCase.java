package com.cleanlearning.application.offering;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.shared.NotFoundException;

/**
 * UC-16 steps 3–4: the admin saves an existing consultation offering. It replaces the offering's fields and packages; a published
 * offering's changes show in the shop at once.
 */
@Service
public class EditConsultationOfferingUseCase {

    private final LockConsultationOfferingPort lockOffering;

    private final SaveConsultationOfferingPort saveOffering;

    EditConsultationOfferingUseCase(LockConsultationOfferingPort lockOffering,
            SaveConsultationOfferingPort saveOffering) {
        this.lockOffering = lockOffering;
        this.saveOffering = saveOffering;
    }

    /**
     * @throws NotFoundException if there's no consultation offering with this id
     * @throws IllegalArgumentException if a package id isn't one of this offering's
     */
    @Transactional
    public ConsultationOffering invoke(OfferingId id, ConsultationOfferingCommand command) {
        ConsultationOffering offering = lockOffering.lock(id)
                .orElseThrow(() -> new NotFoundException("Offering " + id.value() + " not found."));
        ConsultationOffering edited = offering.edit(command.name(), command.description(), command.sessionDuration(),
                command.cancellationDeadline(), command.packages());
        saveOffering.update(edited);
        return edited;
    }
}
