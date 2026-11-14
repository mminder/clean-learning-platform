package com.cleanlearning.adapters.database.consultation;

import com.cleanlearning.adapters.database.consultation.entity.AppointmentEntity;
import com.cleanlearning.adapters.database.consultation.entity.ConsultationCaseEntity;
import com.cleanlearning.adapters.database.consultation.entity.StoredAppointmentStatus;
import com.cleanlearning.domain.consultation.Appointment;
import com.cleanlearning.domain.consultation.CancelledAppointment;
import com.cleanlearning.domain.consultation.CaseCredits;
import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.value.Money;
import java.util.List;
import java.util.Objects;

final class ConsultationCaseMapper {

  private ConsultationCaseMapper() {}

  static CaseCredits toCaseCredits(int sessionCount, List<AppointmentEntity> appointments) {
    return new CaseCredits(
        new SessionCount(sessionCount),
        appointments.stream()
            .filter(appointment -> appointment.getStatus() == StoredAppointmentStatus.BOOKED)
            .map(ConsultationCaseMapper::toAppointment)
            .toList(),
        appointments.stream()
            .filter(appointment -> appointment.getStatus() == StoredAppointmentStatus.CANCELLED)
            .map(ConsultationCaseMapper::toCancelledAppointment)
            .toList());
  }

  static PurchaseTerms toPurchaseTerms(ConsultationCaseEntity entity) {
    return new PurchaseTerms(
        new Money(entity.getPricePaid()),
        new SessionCount(entity.getSessionCount()),
        new SessionDuration(entity.getSessionDurationMinutes()),
        new CancellationDeadline(entity.getCancellationDeadlineHours()));
  }

  private static Appointment toAppointment(AppointmentEntity entity) {
    return new Appointment(
        new AppointmentId(entity.getId()),
        new ConsultationCaseId(entity.getConsultationCaseId()),
        entity.getStartsAt(),
        entity.getEndsAt());
  }

  /** The column's check says a cancelled appointment has {@code credit_refunded} (V9). */
  private static CancelledAppointment toCancelledAppointment(AppointmentEntity entity) {
    return new CancelledAppointment(
        new AppointmentId(entity.getId()),
        entity.getStartsAt(),
        entity.getEndsAt(),
        Objects.requireNonNull(entity.getCreditRefunded(), "creditRefunded"));
  }
}
