package com.cleanlearning.adapters.database.consultation;

import com.cleanlearning.adapters.database.consultation.entity.AppointmentEntity;
import com.cleanlearning.adapters.database.consultation.entity.ConsultationCaseEntity;
import com.cleanlearning.adapters.database.consultation.entity.StoredAppointmentStatus;
import com.cleanlearning.adapters.database.offering.row.EligibleExpertRow;
import com.cleanlearning.application.consultation.port.LoadBusyTimesPort;
import com.cleanlearning.application.consultation.port.LoadCustomerBusyTimesPort;
import com.cleanlearning.application.consultation.port.LoadEligibleExpertsPort;
import com.cleanlearning.application.consultation.port.SaveBookingPort;
import com.cleanlearning.domain.consultation.Appointment;
import com.cleanlearning.domain.consultation.outcome.Booking;
import com.cleanlearning.domain.consultation.value.BusyTime;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.offering.value.OfferingId;
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
    implements LoadEligibleExpertsPort,
        LoadBusyTimesPort,
        LoadCustomerBusyTimesPort,
        SaveBookingPort {

  private final ConsultationCaseRepository repository;

  private final EntityManager entityManager;

  ConsultationBookingAdapter(ConsultationCaseRepository repository, EntityManager entityManager) {
    this.repository = repository;
    this.entityManager = entityManager;
  }

  /** The offering isn't locked: eligibility is read when the booking runs. */
  @Override
  public List<ExpertProfile> load(OfferingId offeringId) {
    return repository.findEligibleExperts(offeringId.value()).stream()
        .map(ConsultationBookingAdapter::toEligibleExpert)
        .toList();
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

  private static ExpertProfile toEligibleExpert(EligibleExpertRow row) {
    return new ExpertProfile(
        new UserId(row.id()),
        new DisplayName(row.displayName()),
        row.bio(),
        Optional.ofNullable(row.photoId()).map(ImageId::new));
  }

  private static BusyTime toBusyTime(AppointmentEntity entity) {
    return new BusyTime(entity.getStartsAt(), entity.getEndsAt());
  }
}
