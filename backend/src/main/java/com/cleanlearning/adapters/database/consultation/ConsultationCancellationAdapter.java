package com.cleanlearning.adapters.database.consultation;

import com.cleanlearning.adapters.database.consultation.entity.AppointmentEntity;
import com.cleanlearning.adapters.database.consultation.entity.ConsultationCaseEntity;
import com.cleanlearning.adapters.database.consultation.entity.StoredAppointmentStatus;
import com.cleanlearning.application.consultation.port.SaveAppointmentCancellationPort;
import com.cleanlearning.application.consultation.port.SaveCaseCancellationPort;
import com.cleanlearning.domain.consultation.outcome.AppointmentCancellation;
import com.cleanlearning.domain.consultation.outcome.CaseCancellation;
import com.cleanlearning.domain.consultation.value.CaseStatus;
import jakarta.persistence.EntityManager;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Changes the entities that {@code ConsultationCaseAdapter} loaded under the case's lock. */
@Component
class ConsultationCancellationAdapter
    implements SaveAppointmentCancellationPort, SaveCaseCancellationPort {

  private final EntityManager entityManager;

  ConsultationCancellationAdapter(EntityManager entityManager) {
    this.entityManager = entityManager;
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
}
