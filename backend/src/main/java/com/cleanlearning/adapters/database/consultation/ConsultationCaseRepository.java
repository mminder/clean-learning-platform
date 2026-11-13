package com.cleanlearning.adapters.database.consultation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import com.cleanlearning.adapters.database.consultation.entity.AppointmentEntity;
import com.cleanlearning.adapters.database.consultation.entity.ConsultationCaseEntity;
import com.cleanlearning.adapters.database.consultation.row.CustomerConsultationCaseRow;
import com.cleanlearning.adapters.database.consultation.row.CustomerConsultationCaseSummaryRow;
import com.cleanlearning.adapters.database.consultation.row.ForfeitedCreditsRow;
import com.cleanlearning.adapters.database.offering.entity.ConsultationPackageEntity;
import com.cleanlearning.adapters.database.offering.row.EligibleExpertRow;

import jakarta.persistence.LockModeType;

interface ConsultationCaseRepository extends Repository<ConsultationCaseEntity, UUID> {

    /**
     * The customer's case with its offering, in every status of the offering: an archived offering's cases keep
     * working (UC-16). Its assigned expert's profile, if it has one. Scoped to the customer in SQL, so another
     * customer's case isn't found (security.md).
     */
    @Query("""
            select new com.cleanlearning.adapters.database.consultation.row.CustomerConsultationCaseRow(
                c.id, o.name, o.imageId, c.pricePaid, c.sessionCount, c.sessionDurationMinutes,
                c.cancellationDeadlineHours, c.purchasedAt, p.userId, p.displayName, p.bio)
            from ConsultationCaseEntity c
            join OfferingEntity o on o.id = c.consultationOfferingId
            left join ExpertProfileEntity p on p.userId = c.assignedExpertId
            where c.id = :id and c.customerId = :customerId
            """)
    Optional<CustomerConsultationCaseRow> findCustomerCase(UUID id, UUID customerId);

    /**
     * The customer's cases with their offerings, in every status of the offering (UC-16), and their assigned experts'
     * display names, in no particular order: the use case sorts them (D-17). Scoped to the customer in SQL
     * (security.md).
     */
    @Query("""
            select new com.cleanlearning.adapters.database.consultation.row.CustomerConsultationCaseSummaryRow(
                c.id, o.name, o.imageId, c.sessionCount, c.purchasedAt, p.displayName)
            from ConsultationCaseEntity c
            join OfferingEntity o on o.id = c.consultationOfferingId
            left join ExpertProfileEntity p on p.userId = c.assignedExpertId
            where c.customerId = :customerId
            """)
    List<CustomerConsultationCaseSummaryRow> findCustomerCases(UUID customerId);

    /** The offering's packages, which a purchase chooses from (D-23: this adapter may read the offering's tables). */
    @Query("select p from ConsultationPackageEntity p where p.consultationOfferingId = :consultationOfferingId")
    List<ConsultationPackageEntity> findPackageEntities(UUID consultationOfferingId);

    /** The customer's case, as an entity, in every status of its offering. Scoped to the customer in SQL. */
    @Query("select c from ConsultationCaseEntity c where c.id = :id and c.customerId = :customerId")
    Optional<ConsultationCaseEntity> findCustomerCaseEntity(UUID id, UUID customerId);

    /**
     * Like {@link #findCustomerCaseEntity}, with {@code PESSIMISTIC_WRITE} on the case's row
     * ({@code select … for no key update}), waiting for every other lock of the row. Scoped to the customer in SQL,
     * so another customer's case is neither found nor locked.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConsultationCaseEntity c where c.id = :id and c.customerId = :customerId")
    Optional<ConsultationCaseEntity> lockCustomerCaseEntity(UUID id, UUID customerId);

    /** The case's {@code BOOKED} appointments, consumed or not: each uses a credit (spec 04, decision 15). */
    @Query("""
            select count(a) from AppointmentEntity a
            where a.consultationCaseId = :consultationCaseId
              and a.status = com.cleanlearning.adapters.database.consultation.entity.StoredAppointmentStatus.BOOKED
            """)
    long countBookedAppointments(UUID consultationCaseId);

    /**
     * The case's forfeited credits: its appointments cancelled late (UC-07), which the open credits lack (glossary).
     */
    @Query("""
            select count(a) from AppointmentEntity a
            where a.consultationCaseId = :consultationCaseId
              and a.status = com.cleanlearning.adapters.database.consultation.entity.StoredAppointmentStatus.CANCELLED
              and a.creditRefunded = false
            """)
    long countForfeitedCredits(UUID consultationCaseId);

    /**
     * The forfeited credits of all the customer's cases, one row per case that has any: one query for the whole list.
     * Scoped to the customer in SQL, through the case (security.md).
     */
    @Query("""
            select new com.cleanlearning.adapters.database.consultation.row.ForfeitedCreditsRow(
                a.consultationCaseId, count(a))
            from AppointmentEntity a
            join ConsultationCaseEntity c on c.id = a.consultationCaseId
            where c.customerId = :customerId
              and a.status = com.cleanlearning.adapters.database.consultation.entity.StoredAppointmentStatus.CANCELLED
              and a.creditRefunded = false
            group by a.consultationCaseId
            """)
    List<ForfeitedCreditsRow> findForfeitedCreditsOfCustomer(UUID customerId);

    /**
     * Every appointment of the case, {@code BOOKED} and {@code CANCELLED}, in any order: a cancellation tells a
     * cancelled appointment from an unknown one, and counts the forfeited credits.
     */
    @Query("select a from AppointmentEntity a where a.consultationCaseId = :consultationCaseId")
    List<AppointmentEntity> findAppointments(UUID consultationCaseId);

    /** The case's {@code BOOKED} appointments, in any order; the domain sorts them. */
    @Query("""
            select a from AppointmentEntity a
            where a.consultationCaseId = :consultationCaseId
              and a.status = com.cleanlearning.adapters.database.consultation.entity.StoredAppointmentStatus.BOOKED
            """)
    List<AppointmentEntity> findBookedAppointments(UUID consultationCaseId);

    /**
     * The {@code BOOKED} appointments of all the customer's cases, in any order; the adapter groups them by case and
     * the domain sorts them. One query for the whole list. Scoped to the customer in SQL, through the case
     * (security.md).
     */
    @Query("""
            select a from AppointmentEntity a
            join ConsultationCaseEntity c on c.id = a.consultationCaseId
            where c.customerId = :customerId
              and a.status = com.cleanlearning.adapters.database.consultation.entity.StoredAppointmentStatus.BOOKED
            """)
    List<AppointmentEntity> findBookedAppointmentsOfCustomer(UUID customerId);

    /**
     * The {@code BOOKED} appointments of every case assigned to the expert, of any customer, that end after
     * {@code from}: the expert's busy time (UC-06). An appointment's expert is its case's assigned expert.
     */
    @Query("""
            select a from AppointmentEntity a
            join ConsultationCaseEntity c on c.id = a.consultationCaseId
            where c.assignedExpertId = :expertId
              and a.status = com.cleanlearning.adapters.database.consultation.entity.StoredAppointmentStatus.BOOKED
              and a.endsAt > :from
            """)
    List<AppointmentEntity> findBookedAppointmentsOfExpert(UUID expertId, Instant from);

    /**
     * The {@code BOOKED} appointments of all the customer's cases, with any expert, that end after {@code from}: the
     * customer's busy time (spec 04, decision 22). Scoped to the customer in SQL, through the case (security.md).
     */
    @Query("""
            select a from AppointmentEntity a
            join ConsultationCaseEntity c on c.id = a.consultationCaseId
            where c.customerId = :customerId
              and a.status = com.cleanlearning.adapters.database.consultation.entity.StoredAppointmentStatus.BOOKED
              and a.endsAt > :from
            """)
    List<AppointmentEntity> findBookedAppointmentsOfCustomerEndingAfter(UUID customerId, Instant from);

    /**
     * The offering's current eligible experts with their profiles, sorted by display name (the column is ICU-collated,
     * so alphabetically), then id. D-23: this adapter may read the offering's and the user's tables.
     */
    @Query("""
            select new com.cleanlearning.adapters.database.offering.row.EligibleExpertRow(
                p.userId, p.displayName, p.bio)
            from EligibleExpertEntity e
            join ExpertProfileEntity p on p.userId = e.expertId
            where e.consultationOfferingId = :consultationOfferingId
            order by p.displayName, p.userId
            """)
    List<EligibleExpertRow> findEligibleExperts(UUID consultationOfferingId);
}
