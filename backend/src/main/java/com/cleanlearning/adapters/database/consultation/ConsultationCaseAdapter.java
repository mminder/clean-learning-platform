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
import com.cleanlearning.adapters.database.consultation.row.ForfeitedCreditsRow;
import com.cleanlearning.adapters.database.offering.entity.ConsultationOfferingEntity;
import com.cleanlearning.adapters.database.offering.entity.ConsultationPackageEntity;
import com.cleanlearning.adapters.database.offering.entity.OfferingEntity;
import com.cleanlearning.adapters.database.offering.row.EligibleExpertRow;
import com.cleanlearning.application.consultation.port.LoadBusyTimesPort;
import com.cleanlearning.application.consultation.port.LoadCaseForBookingPort;
import com.cleanlearning.application.consultation.port.LoadCustomerBusyTimesPort;
import com.cleanlearning.application.consultation.port.LoadCustomerConsultationCasePort;
import com.cleanlearning.application.consultation.port.LoadCustomerConsultationCasesPort;
import com.cleanlearning.application.consultation.port.LockCaseForBookingPort;
import com.cleanlearning.application.consultation.port.LockCaseForCancellationPort;
import com.cleanlearning.application.consultation.port.LockConsultationOfferingForPurchasePort;
import com.cleanlearning.application.consultation.port.SaveAppointmentCancellationPort;
import com.cleanlearning.application.consultation.port.SaveBookingPort;
import com.cleanlearning.application.consultation.port.SaveCaseCancellationPort;
import com.cleanlearning.application.consultation.port.SaveConsultationCasePort;
import com.cleanlearning.domain.consultation.Appointment;
import com.cleanlearning.domain.consultation.AppointmentCancellation;
import com.cleanlearning.domain.consultation.AssignedExpert;
import com.cleanlearning.domain.consultation.Booking;
import com.cleanlearning.domain.consultation.CaseCancellation;
import com.cleanlearning.domain.consultation.CaseForBooking;
import com.cleanlearning.domain.consultation.CaseForCancellation;
import com.cleanlearning.domain.consultation.ConsultationCase;
import com.cleanlearning.domain.consultation.ConsultationOfferingForPurchase;
import com.cleanlearning.domain.consultation.CustomerConsultationCase;
import com.cleanlearning.domain.consultation.CustomerConsultationCaseSummary;
import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.BusyTime;
import com.cleanlearning.domain.consultation.value.CancellationOutcome;
import com.cleanlearning.domain.consultation.value.CaseStatus;
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
 * The cases and their appointments (UC-03, UC-05, UC-06, UC-07, UC-08). It reads the offering's tables, through the
 * offering's entities, for the purchase, to show a case's offering live and for the eligible experts, and the expert
 * profiles for the assigned expert (D-23). Every query that lists appointments reads only {@code BOOKED} ones (spec 04,
 * decision 15), so a cancelled one frees its time and leaves the customer's views; the credits count the forfeited ones
 * in SQL, and only the cancellation reads the cancelled ones.
 */
@Component
class ConsultationCaseAdapter implements LockConsultationOfferingForPurchasePort, SaveConsultationCasePort,
        LoadCustomerConsultationCasePort, LoadCustomerConsultationCasesPort, LoadCaseForBookingPort,
        LockCaseForBookingPort, LoadBusyTimesPort, LoadCustomerBusyTimesPort, SaveBookingPort,
        LockCaseForCancellationPort, SaveAppointmentCancellationPort, SaveCaseCancellationPort {

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
     * The query flushes a case created in the same transaction first, so the purchase answers its new case, and so
     * does a cancellation's change of an appointment, so the cancellation answers the case without it. The
     * appointments and the forfeited credits come with two more queries, once the case is found as the customer's.
     */
    @Override
    public Optional<CustomerConsultationCase> load(ConsultationCaseId id, UserId customerId, Instant asOf) {
        return repository.findCustomerCase(id.value(), customerId.value())
                .map(row -> toCustomerConsultationCase(row, repository.findBookedAppointments(row.id()),
                        Math.toIntExact(repository.countForfeitedCredits(row.id())), asOf));
    }

    /**
     * The customer's cases with one query, the appointments of all of them with one more, and their forfeited credits
     * with a third, grouped by case here: a case without appointments gets none, and one without forfeited credits 0.
     */
    @Override
    public List<CustomerConsultationCaseSummary> loadAll(UserId customerId, Instant asOf) {
        Map<UUID, List<Appointment>> appointmentsByCase = repository
                .findBookedAppointmentsOfCustomer(customerId.value()).stream()
                .collect(Collectors.groupingBy(AppointmentEntity::getConsultationCaseId,
                        Collectors.mapping(ConsultationCaseAdapter::toAppointment, Collectors.toList())));
        Map<UUID, Integer> forfeitedCreditsByCase = repository.findForfeitedCreditsOfCustomer(customerId.value())
                .stream().collect(Collectors.toMap(ForfeitedCreditsRow::consultationCaseId,
                        row -> Math.toIntExact(row.count())));
        return repository.findCustomerCases(customerId.value()).stream()
                .map(row -> toCustomerConsultationCaseSummary(row,
                        appointmentsByCase.getOrDefault(row.id(), List.of()),
                        forfeitedCreditsByCase.getOrDefault(row.id(), 0), asOf))
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
                .map(ConsultationCaseAdapter::toBusyTime).toList();
    }

    @Override
    public List<BusyTime> loadCustomerBusyTimes(UserId customerId, Instant from) {
        return repository.findBookedAppointmentsOfCustomerEndingAfter(customerId.value(), from).stream()
                .map(ConsultationCaseAdapter::toBusyTime).toList();
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

    /**
     * {@code PESSIMISTIC_WRITE} on the case's row through the query scoped to the customer that the booking uses
     * ({@code select … for no key update}); then, under the lock, all its appointments, booked and cancelled, which
     * stay managed, so {@link #save(AppointmentCancellation)} changes one of them.
     */
    @Override
    public Optional<CaseForCancellation> lockForCancellation(ConsultationCaseId id, UserId customerId) {
        return repository.lockCustomerCaseEntity(id.value(), customerId.value()).map(this::toCaseForCancellation);
    }

    /** Changes the appointment's managed entity, which {@link #lockForCancellation} loaded in the same transaction. */
    @Override
    public void save(AppointmentCancellation cancellation) {
        AppointmentEntity appointment = Objects.requireNonNull(
                entityManager.find(AppointmentEntity.class, cancellation.appointmentId().value()),
                "The cancelled appointment");
        appointment.setStatus(StoredAppointmentStatus.CANCELLED);
        appointment.setCreditRefunded(cancellation.creditRefunded());
    }

    /**
     * Changes the case's managed entity and those of the appointments cancelled with it, which
     * {@link #lockForCancellation} loaded in the same transaction.
     */
    @Override
    public void save(CaseCancellation cancellation) {
        ConsultationCaseEntity consultationCase = Objects.requireNonNull(
                entityManager.find(ConsultationCaseEntity.class, cancellation.id().value()), "The cancelled case");
        consultationCase.setStatus(CaseStatus.CANCELLED);
        consultationCase.setCancelledAt(cancellation.outcome().cancelledAt());
        consultationCase.setRefundEligible(cancellation.outcome().refundEligible());
        cancellation.appointments().forEach(this::save);
    }

    private CaseForBooking toCaseForBooking(ConsultationCaseEntity entity) {
        return new CaseForBooking(new ConsultationCaseId(entity.getId()), entity.getStatus(), toPurchaseTerms(entity),
                Optional.ofNullable(entity.getAssignedExpertId()).map(UserId::new),
                Math.toIntExact(repository.countBookedAppointments(entity.getId())),
                Math.toIntExact(repository.countForfeitedCredits(entity.getId())),
                repository.findEligibleExperts(entity.getConsultationOfferingId()).stream()
                        .map(ConsultationCaseAdapter::toEligibleExpert).toList());
    }

    private CaseForCancellation toCaseForCancellation(ConsultationCaseEntity entity) {
        List<AppointmentEntity> appointments = repository.findAppointments(entity.getId());
        return new CaseForCancellation(new ConsultationCaseId(entity.getId()), entity.getStatus(),
                toPurchaseTerms(entity),
                appointments.stream().filter(appointment -> appointment.getStatus() == StoredAppointmentStatus.BOOKED)
                        .map(ConsultationCaseAdapter::toAppointment).toList(),
                appointments.stream()
                        .filter(appointment -> appointment.getStatus() == StoredAppointmentStatus.CANCELLED)
                        .map(ConsultationCaseAdapter::toAppointmentCancellation).toList());
    }

    private static PurchaseTerms toPurchaseTerms(ConsultationCaseEntity entity) {
        return new PurchaseTerms(new Money(entity.getPricePaid()), new SessionCount(entity.getSessionCount()),
                new SessionDuration(entity.getSessionDurationMinutes()),
                new CancellationDeadline(entity.getCancellationDeadlineHours()));
    }

    /** The column's check says a cancelled appointment has {@code credit_refunded} (V9). */
    private static AppointmentCancellation toAppointmentCancellation(AppointmentEntity entity) {
        return new AppointmentCancellation(new AppointmentId(entity.getId()),
                Objects.requireNonNull(entity.getCreditRefunded(), "creditRefunded"));
    }

    private static EligibleExpert toEligibleExpert(EligibleExpertRow row) {
        return new EligibleExpert(new UserId(row.id()), new DisplayName(row.displayName()), row.bio());
    }

    private static ConsultationPackage toConsultationPackage(ConsultationPackageEntity entity) {
        return new ConsultationPackage(new ConsultationPackageId(entity.getId()),
                new SessionCount(entity.getSessionCount()), new Money(entity.getPrice()));
    }

    private static CustomerConsultationCase toCustomerConsultationCase(CustomerConsultationCaseRow row,
            List<AppointmentEntity> appointments, int forfeitedCredits, Instant asOf) {
        return new CustomerConsultationCase(new ConsultationCaseId(row.id()), new OfferingName(row.offeringName()),
                Optional.ofNullable(row.offeringImageId()).map(ImageId::new),
                new PurchaseTerms(new Money(row.pricePaid()), new SessionCount(row.sessionCount()),
                        new SessionDuration(row.sessionDurationMinutes()),
                        new CancellationDeadline(row.cancellationDeadlineHours())),
                row.purchasedAt(), assignedExpert(row),
                appointments.stream().map(ConsultationCaseAdapter::toAppointment).toList(), forfeitedCredits,
                cancellation(row), asOf);
    }

    /** V10's checks say a cancelled case has both {@code cancelled_at} and {@code refund_eligible}. */
    private static Optional<CancellationOutcome> cancellation(CustomerConsultationCaseRow row) {
        if (row.status() == CaseStatus.ACTIVE) {
            return Optional.empty();
        }
        return Optional.of(new CancellationOutcome(Objects.requireNonNull(row.cancelledAt(), "cancelledAt"),
                Objects.requireNonNull(row.refundEligible(), "refundEligible")));
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

    private static BusyTime toBusyTime(AppointmentEntity entity) {
        return new BusyTime(entity.getStartsAt(), entity.getEndsAt());
    }

    private static Appointment toAppointment(AppointmentEntity entity) {
        return new Appointment(new AppointmentId(entity.getId()),
                new ConsultationCaseId(entity.getConsultationCaseId()), entity.getStartsAt(), entity.getEndsAt());
    }

    private static CustomerConsultationCaseSummary toCustomerConsultationCaseSummary(
            CustomerConsultationCaseSummaryRow row, List<Appointment> appointments, int forfeitedCredits,
            Instant asOf) {
        return new CustomerConsultationCaseSummary(new ConsultationCaseId(row.id()),
                new OfferingName(row.offeringName()), Optional.ofNullable(row.offeringImageId()).map(ImageId::new),
                new SessionCount(row.sessionCount()), row.purchasedAt(),
                Optional.ofNullable(row.expertDisplayName()).map(DisplayName::new), appointments, forfeitedCredits,
                Optional.ofNullable(row.cancelledAt()), asOf);
    }
}
