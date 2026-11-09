package com.cleanlearning.adapters.database.consultation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import com.cleanlearning.adapters.database.consultation.entity.ConsultationCaseEntity;
import com.cleanlearning.adapters.database.consultation.row.CustomerConsultationCaseRow;
import com.cleanlearning.adapters.database.offering.entity.ConsultationPackageEntity;

interface ConsultationCaseRepository extends Repository<ConsultationCaseEntity, UUID> {

    /**
     * The customer's case with its offering, in every status of the offering: an archived offering's cases keep
     * working (UC-16). Scoped to the customer in SQL, so another customer's case isn't found (security.md).
     */
    @Query("""
            select new com.cleanlearning.adapters.database.consultation.row.CustomerConsultationCaseRow(
                c.id, o.name, o.imageId, c.pricePaid, c.sessionCount, c.sessionDurationMinutes,
                c.cancellationDeadlineHours, c.purchasedAt)
            from ConsultationCaseEntity c
            join OfferingEntity o on o.id = c.consultationOfferingId
            where c.id = :id and c.customerId = :customerId
            """)
    Optional<CustomerConsultationCaseRow> findCustomerCase(UUID id, UUID customerId);

    /** The offering's packages, which a purchase chooses from (D-23: this adapter may read the offering's tables). */
    @Query("select p from ConsultationPackageEntity p where p.consultationOfferingId = :consultationOfferingId")
    List<ConsultationPackageEntity> findPackageEntities(UUID consultationOfferingId);
}
