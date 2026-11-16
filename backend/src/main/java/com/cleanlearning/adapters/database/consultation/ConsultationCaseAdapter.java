package com.cleanlearning.adapters.database.consultation;

import com.cleanlearning.adapters.database.consultation.entity.ConsultationCaseEntity;
import com.cleanlearning.application.consultation.port.LoadConsultationCasePort;
import com.cleanlearning.application.consultation.port.LockConsultationCasePort;
import com.cleanlearning.domain.consultation.ConsultationCase;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.user.value.UserId;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class ConsultationCaseAdapter implements LoadConsultationCasePort, LockConsultationCasePort {

  private final ConsultationCaseRepository repository;

  ConsultationCaseAdapter(ConsultationCaseRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<ConsultationCase> load(ConsultationCaseId id, UserId customerId) {
    return repository
        .findCustomerCaseEntity(id.value(), customerId.value())
        .map(this::toConsultationCase);
  }

  /**
   * Loads all the case's appointments under the lock; they stay managed, so a save changes one of
   * them.
   */
  @Override
  public Optional<ConsultationCase> lock(ConsultationCaseId id, UserId customerId) {
    return repository
        .lockCustomerCaseEntity(id.value(), customerId.value())
        .map(this::toConsultationCase);
  }

  private ConsultationCase toConsultationCase(ConsultationCaseEntity entity) {
    return new ConsultationCase(
        new ConsultationCaseId(entity.getId()),
        new OfferingId(entity.getConsultationOfferingId()),
        entity.getStatus(),
        ConsultationCaseMapper.toPurchaseTerms(entity),
        Optional.ofNullable(entity.getAssignedExpertId()).map(UserId::new),
        ConsultationCaseMapper.toCaseCredits(
            entity.getSessionCount(), repository.findAppointments(entity.getId())));
  }
}
