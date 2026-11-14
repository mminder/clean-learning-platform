package com.cleanlearning.adapters.database.consultation;

import com.cleanlearning.adapters.database.consultation.entity.AppointmentEntity;
import com.cleanlearning.adapters.database.consultation.entity.ConsultationCaseEntity;
import com.cleanlearning.adapters.database.consultation.entity.StoredAppointmentStatus;
import com.cleanlearning.application.consultation.port.LockCaseForCancellationPort;
import com.cleanlearning.application.consultation.port.SaveAppointmentCancellationPort;
import com.cleanlearning.application.consultation.port.SaveCaseCancellationPort;
import com.cleanlearning.domain.consultation.CaseForCancellation;
import com.cleanlearning.domain.consultation.outcome.AppointmentCancellation;
import com.cleanlearning.domain.consultation.outcome.CaseCancellation;
import com.cleanlearning.domain.consultation.value.CaseStatus;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.user.value.UserId;
import jakarta.persistence.EntityManager;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class ConsultationCancellationAdapter
    implements LockCaseForCancellationPort,
        SaveAppointmentCancellationPort,
        SaveCaseCancellationPort {

  private final ConsultationCaseRepository repository;

  private final EntityManager entityManager;

  ConsultationCancellationAdapter(
      ConsultationCaseRepository repository, EntityManager entityManager) {
    this.repository = repository;
    this.entityManager = entityManager;
  }

  /**
   * Loads all the case's appointments under the lock; they stay managed, so a save changes one of
   * them.
   */
  @Override
  public Optional<CaseForCancellation> lock(ConsultationCaseId id, UserId customerId) {
    return repository
        .lockCustomerCaseEntity(id.value(), customerId.value())
        .map(this::toCaseForCancellation);
  }

  @Override
  public void save(AppointmentCancellation cancellation) {
    AppointmentEntity appointment =
        Objects.requireNonNull(
            entityManager.find(AppointmentEntity.class, cancellation.appointmentId().value()),
            "The cancelled appointment");
    appointment.setStatus(StoredAppointmentStatus.CANCELLED);
    appointment.setCreditRefunded(cancellation.creditRefunded());
  }

  @Override
  public void save(CaseCancellation cancellation) {
    ConsultationCaseEntity consultationCase =
        Objects.requireNonNull(
            entityManager.find(ConsultationCaseEntity.class, cancellation.id().value()),
            "The cancelled case");
    consultationCase.setStatus(CaseStatus.CANCELLED);
    consultationCase.setCancelledAt(cancellation.outcome().cancelledAt());
    consultationCase.setRefundEligible(cancellation.outcome().refundEligible());
    cancellation.appointments().forEach(this::save);
  }

  private CaseForCancellation toCaseForCancellation(ConsultationCaseEntity entity) {
    return new CaseForCancellation(
        new ConsultationCaseId(entity.getId()),
        entity.getStatus(),
        ConsultationCaseMapper.toPurchaseTerms(entity),
        ConsultationCaseMapper.toCaseCredits(
            entity.getSessionCount(), repository.findAppointments(entity.getId())));
  }
}
