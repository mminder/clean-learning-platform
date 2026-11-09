package com.cleanlearning.adapters.database.consultation;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cleanlearning.adapters.database.consultation.entity.ConsultationCaseEntity;
import com.cleanlearning.adapters.database.consultation.row.CustomerConsultationCaseRow;
import com.cleanlearning.adapters.database.offering.entity.ConsultationOfferingEntity;
import com.cleanlearning.adapters.database.offering.entity.ConsultationPackageEntity;
import com.cleanlearning.adapters.database.offering.entity.OfferingEntity;
import com.cleanlearning.application.consultation.port.LoadCustomerConsultationCasePort;
import com.cleanlearning.application.consultation.port.LockConsultationOfferingForPurchasePort;
import com.cleanlearning.application.consultation.port.SaveConsultationCasePort;
import com.cleanlearning.domain.consultation.ConsultationCase;
import com.cleanlearning.domain.consultation.ConsultationOfferingForPurchase;
import com.cleanlearning.domain.consultation.CustomerConsultationCase;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.offering.ConsultationPackage;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.value.Money;
import com.cleanlearning.domain.user.value.UserId;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

/**
 * The cases (UC-03, UC-05). It reads the offering's tables, through the offering's entities, for the purchase and to
 * show a case's offering live (D-23).
 */
@Component
class ConsultationCaseAdapter implements LockConsultationOfferingForPurchasePort, SaveConsultationCasePort,
        LoadCustomerConsultationCasePort {

    private final ConsultationCaseRepository repository;

    private final EntityManager entityManager;

    ConsultationCaseAdapter(ConsultationCaseRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    /**
     * {@code PESSIMISTIC_WRITE} on the {@code offering} row, as {@code ConsultationOfferingAdapter.lock} takes it
     * ({@code select … for no key update}); then its {@code consultation_offering} row and its packages. A training
     * offering (UC-17) has no {@code consultation_offering} row, so it isn't found here.
     */
    @Override
    public Optional<ConsultationOfferingForPurchase> lock(OfferingId id) {
        OfferingEntity offering = entityManager.find(OfferingEntity.class, id.value(),
                LockModeType.PESSIMISTIC_WRITE);
        if (offering == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entityManager.find(ConsultationOfferingEntity.class, offering.getId()))
                .map(consultationOffering -> new ConsultationOfferingForPurchase(id, offering.getStatus(),
                        new SessionDuration(consultationOffering.getSessionDurationMinutes()),
                        new CancellationDeadline(consultationOffering.getCancellationDeadlineHours()),
                        repository.findPackageEntities(offering.getId()).stream()
                                .map(ConsultationCaseAdapter::toConsultationPackage).toList()));
    }

    /** A new row, so it's persisted, not merged (backend.md, persistence). */
    @Override
    public void create(ConsultationCase consultationCase) {
        PurchaseTerms terms = consultationCase.purchaseTerms();
        entityManager.persist(new ConsultationCaseEntity(consultationCase.id().value(),
                consultationCase.customerId().value(), consultationCase.offeringId().value(),
                terms.pricePaid().amount(), terms.sessionCount().value(), terms.sessionDuration().minutes(),
                terms.cancellationDeadline().hours(), consultationCase.purchasedAt()));
    }

    /** The query flushes a case created in the same transaction first, so the purchase answers its new case. */
    @Override
    public Optional<CustomerConsultationCase> load(ConsultationCaseId id, UserId customerId) {
        return repository.findCustomerCase(id.value(), customerId.value())
                .map(ConsultationCaseAdapter::toCustomerConsultationCase);
    }

    private static ConsultationPackage toConsultationPackage(ConsultationPackageEntity entity) {
        return new ConsultationPackage(new ConsultationPackageId(entity.getId()),
                new SessionCount(entity.getSessionCount()), new Money(entity.getPrice()));
    }

    private static CustomerConsultationCase toCustomerConsultationCase(CustomerConsultationCaseRow row) {
        return new CustomerConsultationCase(new ConsultationCaseId(row.id()), new OfferingName(row.offeringName()),
                Optional.ofNullable(row.offeringImageId()).map(ImageId::new),
                new PurchaseTerms(new Money(row.pricePaid()), new SessionCount(row.sessionCount()),
                        new SessionDuration(row.sessionDurationMinutes()),
                        new CancellationDeadline(row.cancellationDeadlineHours())),
                row.purchasedAt());
    }
}
