package com.cleanlearning.adapters.database.consultation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cleanlearning.adapters.database.consultation.entity.AppointmentEntity;
import com.cleanlearning.adapters.database.consultation.entity.ConsultationCaseEntity;
import com.cleanlearning.adapters.database.consultation.entity.StoredAppointmentStatus;
import com.cleanlearning.adapters.database.consultation.row.CustomerConsultationCaseRow;
import com.cleanlearning.adapters.database.consultation.row.CustomerConsultationCaseSummaryRow;
import com.cleanlearning.adapters.database.offering.entity.ConsultationOfferingEntity;
import com.cleanlearning.adapters.database.offering.entity.ConsultationPackageEntity;
import com.cleanlearning.adapters.database.offering.entity.OfferingEntity;
import com.cleanlearning.adapters.database.offering.row.EligibleExpertRow;
import com.cleanlearning.application.consultation.port.LoadBusyTimesPort;
import com.cleanlearning.application.consultation.port.LoadCaseForBookingPort;
import com.cleanlearning.application.consultation.port.LoadCustomerConsultationCasePort;
import com.cleanlearning.application.consultation.port.LoadCustomerConsultationCasesPort;
import com.cleanlearning.application.consultation.port.LockCaseForBookingPort;
import com.cleanlearning.application.consultation.port.LockConsultationOfferingForPurchasePort;
import com.cleanlearning.application.consultation.port.SaveBookingPort;
import com.cleanlearning.application.consultation.port.SaveConsultationCasePort;
import com.cleanlearning.domain.consultation.Appointment;
import com.cleanlearning.domain.consultation.AssignedExpert;
import com.cleanlearning.domain.consultation.Booking;
import com.cleanlearning.domain.consultation.CaseForBooking;
import com.cleanlearning.domain.consultation.ConsultationCase;
import com.cleanlearning.domain.consultation.ConsultationOfferingForPurchase;
import com.cleanlearning.domain.consultation.CustomerConsultationCase;
import com.cleanlearning.domain.consultation.CustomerConsultationCaseSummary;
import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.BusyTime;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.offering.ConsultationPackage;
import com.cleanlearning.domain.offering.EligibleExpert;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.value.Money;
import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.UserId;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

/**
 * The cases and their appointments (UC-03, UC-05, UC-06). It reads the offering's tables, through the offering's
 * entities, for the purchase, to show a case's offering live and for the eligible experts, and the expert profiles for
 * the assigned expert (D-23). Every query of appointments reads only {@code BOOKED} ones (spec 04, decision 15).
 */
@Component
class ConsultationCaseAdapter implements LockConsultationOfferingForPurchasePort, SaveConsultationCasePort,
        LoadCustomerConsultationCasePort, LoadCustomerConsultationCasesPort, LoadCaseForBookingPort,
        LockCaseForBookingPort, LoadBusyTimesPort, SaveBookingPort {

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

    /**
     * The query flushes a case created in the same transaction first, so the purchase answers its new case. The
     * appointments come with a second query, once the case is found as the customer's.
     */
    @Override
    public Optional<CustomerConsultationCase> load(ConsultationCaseId id, UserId customerId, Instant asOf) {
        return repository.findCustomerCase(id.value(), customerId.value())
                .map(row -> toCustomerConsultationCase(row, repository.findBookedAppointments(row.id()), asOf));
    }

    /**
     * The customer's cases with one query, and the appointments of all of them with one more, grouped by case here: a
     * case without appointments gets none.
     */
    @Override
    public List<CustomerConsultationCaseSummary> loadAll(UserId customerId, Instant asOf) {
        Map<UUID, List<Appointment>> appointmentsByCase = repository
                .findBookedAppointmentsOfCustomer(customerId.value()).stream()
                .collect(Collectors.groupingBy(AppointmentEntity::getConsultationCaseId,
                        Collectors.mapping(ConsultationCaseAdapter::toAppointment, Collectors.toList())));
        return repository.findCustomerCases(customerId.value()).stream()
                .map(row -> toCustomerConsultationCaseSummary(row,
                        appointmentsByCase.getOrDefault(row.id(), List.of()), asOf))
                .toList();
    }

    @Override
    public Optional<CaseForBooking> load(ConsultationCaseId id, UserId customerId) {
        return repository.findCustomerCaseEntity(id.value(), customerId.value()).map(this::toCaseForBooking);
    }

    /**
     * {@code PESSIMISTIC_WRITE} on the case's row, through a query scoped to the customer ({@code select … for no key
     * update}); then, under the lock, its booked appointments and its offering's eligible experts. The offering isn't
     * locked: eligibility is read when the booking runs (UC-06).
     */
    @Override
    public Optional<CaseForBooking> lock(ConsultationCaseId id, UserId customerId) {
        return repository.lockCustomerCaseEntity(id.value(), customerId.value()).map(this::toCaseForBooking);
    }

    @Override
    public List<BusyTime> load(UserId expertId, Instant from) {
        return repository.findBookedAppointmentsOfExpert(expertId.value(), from).stream()
                .map(appointment -> new BusyTime(appointment.getStartsAt(), appointment.getEndsAt())).toList();
    }

    /**
     * Persists the appointment, and on the first booking sets the assigned expert on the case's managed entity, which
     * {@link #lock} loaded in the same transaction.
     */
    @Override
    public void save(Booking booking) {
        Appointment appointment = booking.appointment();
        entityManager.persist(new AppointmentEntity(appointment.id().value(), appointment.consultationCaseId().value(),
                appointment.start(), appointment.end(), StoredAppointmentStatus.BOOKED));
        if (booking.assignsExpert()) {
            ConsultationCaseEntity consultationCase = Objects.requireNonNull(
                    entityManager.find(ConsultationCaseEntity.class, appointment.consultationCaseId().value()),
                    "The booked case");
            consultationCase.setAssignedExpertId(booking.expertId().value());
        }
    }

    private CaseForBooking toCaseForBooking(ConsultationCaseEntity entity) {
        return new CaseForBooking(new ConsultationCaseId(entity.getId()),
                new PurchaseTerms(new Money(entity.getPricePaid()), new SessionCount(entity.getSessionCount()),
                        new SessionDuration(entity.getSessionDurationMinutes()),
                        new CancellationDeadline(entity.getCancellationDeadlineHours())),
                Optional.ofNullable(entity.getAssignedExpertId()).map(UserId::new),
                Math.toIntExact(repository.countBookedAppointments(entity.getId())),
                repository.findEligibleExperts(entity.getConsultationOfferingId()).stream()
                        .map(ConsultationCaseAdapter::toEligibleExpert).toList());
    }

    private static EligibleExpert toEligibleExpert(EligibleExpertRow row) {
        return new EligibleExpert(new UserId(row.id()), new DisplayName(row.displayName()), row.bio());
    }

    private static ConsultationPackage toConsultationPackage(ConsultationPackageEntity entity) {
        return new ConsultationPackage(new ConsultationPackageId(entity.getId()),
                new SessionCount(entity.getSessionCount()), new Money(entity.getPrice()));
    }

    private static CustomerConsultationCase toCustomerConsultationCase(CustomerConsultationCaseRow row,
            List<AppointmentEntity> appointments, Instant asOf) {
        return new CustomerConsultationCase(new ConsultationCaseId(row.id()), new OfferingName(row.offeringName()),
                Optional.ofNullable(row.offeringImageId()).map(ImageId::new),
                new PurchaseTerms(new Money(row.pricePaid()), new SessionCount(row.sessionCount()),
                        new SessionDuration(row.sessionDurationMinutes()),
                        new CancellationDeadline(row.cancellationDeadlineHours())),
                row.purchasedAt(), assignedExpert(row),
                appointments.stream().map(ConsultationCaseAdapter::toAppointment).toList(), asOf);
    }

    /** The left join's expert columns are all null before the first booking. */
    private static Optional<AssignedExpert> assignedExpert(CustomerConsultationCaseRow row) {
        UUID expertId = row.expertId();
        if (expertId == null) {
            return Optional.empty();
        }
        return Optional.of(new AssignedExpert(new UserId(expertId),
                new DisplayName(Objects.requireNonNull(row.expertDisplayName(), "expertDisplayName")),
                Objects.requireNonNull(row.expertBio(), "expertBio")));
    }

    private static Appointment toAppointment(AppointmentEntity entity) {
        return new Appointment(new AppointmentId(entity.getId()),
                new ConsultationCaseId(entity.getConsultationCaseId()), entity.getStartsAt(), entity.getEndsAt());
    }

    private static CustomerConsultationCaseSummary toCustomerConsultationCaseSummary(
            CustomerConsultationCaseSummaryRow row, List<Appointment> appointments, Instant asOf) {
        return new CustomerConsultationCaseSummary(new ConsultationCaseId(row.id()),
                new OfferingName(row.offeringName()), Optional.ofNullable(row.offeringImageId()).map(ImageId::new),
                new SessionCount(row.sessionCount()), row.purchasedAt(),
                Optional.ofNullable(row.expertDisplayName()).map(DisplayName::new), appointments, asOf);
    }
}
