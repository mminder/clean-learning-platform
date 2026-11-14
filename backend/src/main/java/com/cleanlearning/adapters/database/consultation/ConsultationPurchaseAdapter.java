package com.cleanlearning.adapters.database.consultation;

import com.cleanlearning.adapters.database.consultation.entity.ConsultationCaseEntity;
import com.cleanlearning.adapters.database.offering.entity.ConsultationOfferingEntity;
import com.cleanlearning.adapters.database.offering.entity.ConsultationPackageEntity;
import com.cleanlearning.adapters.database.offering.entity.OfferingEntity;
import com.cleanlearning.application.consultation.port.LockConsultationOfferingForPurchasePort;
import com.cleanlearning.application.consultation.port.SaveConsultationCasePort;
import com.cleanlearning.domain.consultation.ConsultationOfferingForPurchase;
import com.cleanlearning.domain.consultation.outcome.NewConsultationCase;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.offering.ConsultationPackage;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.value.Money;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class ConsultationPurchaseAdapter
    implements LockConsultationOfferingForPurchasePort, SaveConsultationCasePort {

  private final ConsultationCaseRepository repository;

  private final EntityManager entityManager;

  ConsultationPurchaseAdapter(ConsultationCaseRepository repository, EntityManager entityManager) {
    this.repository = repository;
    this.entityManager = entityManager;
  }

  /**
   * Locks the {@code offering} row as {@code ConsultationOfferingAdapter.lock} does. A training
   * offering has no {@code consultation_offering} row, so it isn't found here.
   */
  @Override
  public Optional<ConsultationOfferingForPurchase> lock(OfferingId id) {
    OfferingEntity offering =
        entityManager.find(OfferingEntity.class, id.value(), LockModeType.PESSIMISTIC_WRITE);
    if (offering == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(
            entityManager.find(ConsultationOfferingEntity.class, offering.getId()))
        .map(consultationOffering ->
            new ConsultationOfferingForPurchase(
                id,
                offering.getStatus(),
                new SessionDuration(consultationOffering.getSessionDurationMinutes()),
                new CancellationDeadline(consultationOffering.getCancellationDeadlineHours()),
                repository.findPackageEntities(offering.getId()).stream()
                    .map(ConsultationPurchaseAdapter::toConsultationPackage)
                    .toList()));
  }

  /** A new row, so it's persisted, not merged (backend.md, persistence). */
  @Override
  public void create(NewConsultationCase consultationCase) {
    PurchaseTerms terms = consultationCase.purchaseTerms();
    entityManager.persist(
        new ConsultationCaseEntity(
            consultationCase.id().value(),
            consultationCase.customerId().value(),
            consultationCase.offeringId().value(),
            terms.pricePaid().amount(),
            terms.sessionCount().value(),
            terms.sessionDuration().minutes(),
            terms.cancellationDeadline().hours(),
            consultationCase.purchasedAt()));
  }

  private static ConsultationPackage toConsultationPackage(ConsultationPackageEntity entity) {
    return new ConsultationPackage(
        new ConsultationPackageId(entity.getId()),
        new SessionCount(entity.getSessionCount()),
        new Money(entity.getPrice()));
  }
}
