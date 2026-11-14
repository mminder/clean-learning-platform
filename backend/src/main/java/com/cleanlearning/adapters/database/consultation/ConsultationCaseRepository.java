package com.cleanlearning.adapters.database.consultation;

import com.cleanlearning.adapters.database.consultation.entity.AppointmentEntity;
import com.cleanlearning.adapters.database.consultation.entity.ConsultationCaseEntity;
import com.cleanlearning.adapters.database.consultation.row.CustomerConsultationCaseRow;
import com.cleanlearning.adapters.database.consultation.row.CustomerConsultationCaseSummaryRow;
import com.cleanlearning.adapters.database.offering.entity.ConsultationPackageEntity;
import com.cleanlearning.adapters.database.offering.row.EligibleExpertRow;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

interface ConsultationCaseRepository extends Repository<ConsultationCaseEntity, UUID> {

  /**
   * In every status of the offering: an archived offering's cases keep working. Scoped to the
   * customer in SQL, so another customer's case isn't found.
   */
  @Query(
      """
      select new com.cleanlearning.adapters.database.consultation.row.CustomerConsultationCaseRow(
          c.id, o.name, o.imageId, c.pricePaid, c.sessionCount, c.sessionDurationMinutes,
          c.cancellationDeadlineHours, c.purchasedAt, p.userId, p.displayName, p.bio, c.status, c.cancelledAt,
          c.refundEligible)
      from ConsultationCaseEntity c
      join OfferingEntity o on o.id = c.consultationOfferingId
      left join ExpertProfileEntity p on p.userId = c.assignedExpertId
      where c.id = :id and c.customerId = :customerId
      """)
  Optional<CustomerConsultationCaseRow> findCustomerCase(UUID id, UUID customerId);

  /** In no particular order: the use case sorts them (D-17). */
  @Query(
      """
      select new com.cleanlearning.adapters.database.consultation.row.CustomerConsultationCaseSummaryRow(
          c.id, o.name, o.imageId, c.sessionCount, c.purchasedAt, p.displayName, c.cancelledAt)
      from ConsultationCaseEntity c
      join OfferingEntity o on o.id = c.consultationOfferingId
      left join ExpertProfileEntity p on p.userId = c.assignedExpertId
      where c.customerId = :customerId
      """)
  List<CustomerConsultationCaseSummaryRow> findCustomerCases(UUID customerId);

  @Query(
      "select p from ConsultationPackageEntity p where p.consultationOfferingId = :consultationOfferingId")
  List<ConsultationPackageEntity> findPackageEntities(UUID consultationOfferingId);

  @Query("select c from ConsultationCaseEntity c where c.id = :id and c.customerId = :customerId")
  Optional<ConsultationCaseEntity> findCustomerCaseEntity(UUID id, UUID customerId);

  /**
   * Waits for every other lock of the row. Scoped to the customer in SQL, so another customer's
   * case is neither found nor locked.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from ConsultationCaseEntity c where c.id = :id and c.customerId = :customerId")
  Optional<ConsultationCaseEntity> lockCustomerCaseEntity(UUID id, UUID customerId);

  @Query("select a from AppointmentEntity a where a.consultationCaseId = :consultationCaseId")
  List<AppointmentEntity> findAppointments(UUID consultationCaseId);

  @Query(
      """
      select a from AppointmentEntity a
      join ConsultationCaseEntity c on c.id = a.consultationCaseId
      where c.customerId = :customerId
      """)
  List<AppointmentEntity> findAppointmentsOfCustomer(UUID customerId);

  @Query(
      """
      select a from AppointmentEntity a
      join ConsultationCaseEntity c on c.id = a.consultationCaseId
      where c.assignedExpertId = :expertId
        and a.status = com.cleanlearning.adapters.database.consultation.entity.StoredAppointmentStatus.BOOKED
        and a.endsAt > :from
      """)
  List<AppointmentEntity> findBookedAppointmentsOfExpert(UUID expertId, Instant from);

  @Query(
      """
      select a from AppointmentEntity a
      join ConsultationCaseEntity c on c.id = a.consultationCaseId
      where c.customerId = :customerId
        and a.status = com.cleanlearning.adapters.database.consultation.entity.StoredAppointmentStatus.BOOKED
        and a.endsAt > :from
      """)
  List<AppointmentEntity> findBookedAppointmentsOfCustomerEndingAfter(
      UUID customerId, Instant from);

  /** Sorted by display name, which the column's ICU collation makes alphabetical, then id. */
  @Query(
      """
      select new com.cleanlearning.adapters.database.offering.row.EligibleExpertRow(
          p.userId, p.displayName, p.bio)
      from EligibleExpertEntity e
      join ExpertProfileEntity p on p.userId = e.expertId
      where e.consultationOfferingId = :consultationOfferingId
      order by p.displayName, p.userId
      """)
  List<EligibleExpertRow> findEligibleExperts(UUID consultationOfferingId);
}
