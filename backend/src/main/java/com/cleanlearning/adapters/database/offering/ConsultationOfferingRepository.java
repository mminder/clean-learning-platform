package com.cleanlearning.adapters.database.offering;

import com.cleanlearning.adapters.database.offering.entity.ConsultationPackageEntity;
import com.cleanlearning.adapters.database.offering.entity.EligibleExpertEntity;
import com.cleanlearning.adapters.database.offering.entity.OfferingEntity;
import com.cleanlearning.adapters.database.offering.row.ConsultationOfferingSummaryRow;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

interface ConsultationOfferingRepository extends Repository<OfferingEntity, UUID> {

  /**
   * The counts are subqueries: joining packages and experts would multiply the rows of one by the
   * other.
   */
  @Query(
      """
      select new com.cleanlearning.adapters.database.offering.row.ConsultationOfferingSummaryRow(
          o.id, o.name, o.status,
          (select count(p) from ConsultationPackageEntity p where p.consultationOfferingId = o.id),
          (select count(e) from EligibleExpertEntity e where e.consultationOfferingId = o.id))
      from OfferingEntity o
      join ConsultationOfferingEntity c on c.offeringId = o.id
      order by o.name, o.id
      """)
  List<ConsultationOfferingSummaryRow> findSummaries();

  @Query(
      "select p from ConsultationPackageEntity p where p.consultationOfferingId = :consultationOfferingId")
  List<ConsultationPackageEntity> findPackageEntities(UUID consultationOfferingId);

  @Query(
      """
      select exists (
          select c.id from ConsultationCaseEntity c where c.consultationOfferingId = :consultationOfferingId)
      """)
  boolean existsCase(UUID consultationOfferingId);

  @Query(
      "select e from EligibleExpertEntity e where e.consultationOfferingId = :consultationOfferingId")
  List<EligibleExpertEntity> findEligibleExpertEntities(UUID consultationOfferingId);
}
