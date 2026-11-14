package com.cleanlearning.adapters.database.consultation;

import com.cleanlearning.adapters.database.consultation.entity.AppointmentEntity;
import com.cleanlearning.adapters.database.consultation.entity.ConsultationCaseEntity;
import com.cleanlearning.adapters.database.consultation.entity.StoredAppointmentStatus;
import com.cleanlearning.adapters.database.offering.row.EligibleExpertRow;
import com.cleanlearning.application.consultation.port.LoadBusyTimesPort;
import com.cleanlearning.application.consultation.port.LoadCaseForBookingPort;
import com.cleanlearning.application.consultation.port.LoadCustomerBusyTimesPort;
import com.cleanlearning.application.consultation.port.LockCaseForBookingPort;
import com.cleanlearning.application.consultation.port.SaveBookingPort;
import com.cleanlearning.domain.consultation.Appointment;
import com.cleanlearning.domain.consultation.CaseForBooking;
import com.cleanlearning.domain.consultation.outcome.Booking;
import com.cleanlearning.domain.consultation.value.BusyTime;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.user.ExpertProfile;
import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.UserId;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** The busy times read only {@code BOOKED} appointments, so a cancelled one frees its time. */
@Component
class ConsultationBookingAdapter
    implements LoadCaseForBookingPort,
        LockCaseForBookingPort,
        LoadBusyTimesPort,
        LoadCustomerBusyTimesPort,
        SaveBookingPort {

  private final ConsultationCaseRepository repository;

  private final EntityManager entityManager;

  ConsultationBookingAdapter(ConsultationCaseRepository repository, EntityManager entityManager) {
    this.repository = repository;
    this.entityManager = entityManager;
  }

  @Override
  public Optional<CaseForBooking> load(ConsultationCaseId id, UserId customerId) {
    return repository
        .findCustomerCaseEntity(id.value(), customerId.value())
        .map(this::toCaseForBooking);
  }

  /** The offering isn't locked: eligibility is read when the booking runs. */
  @Override
  public Optional<CaseForBooking> lock(ConsultationCaseId id, UserId customerId) {
    return repository
        .lockCustomerCaseEntity(id.value(), customerId.value())
        .map(this::toCaseForBooking);
  }

  @Override
  public List<BusyTime> load(UserId expertId, Instant from) {
    return repository.findBookedAppointmentsOfExpert(expertId.value(), from).stream()
        .map(ConsultationBookingAdapter::toBusyTime)
        .toList();
  }

  /** Not named {@code load}: {@link #load(UserId, Instant)} has the same parameters. */
  @Override
  public List<BusyTime> loadCustomerBusyTimes(UserId customerId, Instant from) {
    return repository.findBookedAppointmentsOfCustomerEndingAfter(customerId.value(), from).stream()
        .map(ConsultationBookingAdapter::toBusyTime)
        .toList();
  }

  @Override
  public void save(Booking booking) {
    Appointment appointment = booking.appointment();
    entityManager.persist(
        new AppointmentEntity(
            appointment.id().value(),
            appointment.consultationCaseId().value(),
            appointment.start(),
            appointment.end(),
            StoredAppointmentStatus.BOOKED));
    if (booking.assignsExpert()) {
      ConsultationCaseEntity consultationCase =
          Objects.requireNonNull(
              entityManager.find(
                  ConsultationCaseEntity.class, appointment.consultationCaseId().value()),
              "The booked case");
      consultationCase.setAssignedExpertId(booking.expertId().value());
    }
  }

  private CaseForBooking toCaseForBooking(ConsultationCaseEntity entity) {
    return new CaseForBooking(
        new ConsultationCaseId(entity.getId()),
        entity.getStatus(),
        ConsultationCaseMapper.toPurchaseTerms(entity),
        Optional.ofNullable(entity.getAssignedExpertId()).map(UserId::new),
        ConsultationCaseMapper.toCaseCredits(
            entity.getSessionCount(), repository.findAppointments(entity.getId())),
        repository.findEligibleExperts(entity.getConsultationOfferingId()).stream()
            .map(ConsultationBookingAdapter::toEligibleExpert)
            .toList());
  }

  private static ExpertProfile toEligibleExpert(EligibleExpertRow row) {
    return new ExpertProfile(new UserId(row.id()), new DisplayName(row.displayName()), row.bio());
  }

  private static BusyTime toBusyTime(AppointmentEntity entity) {
    return new BusyTime(entity.getStartsAt(), entity.getEndsAt());
  }
}
