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
   * Every consultation offering in every status, in one query. The counts are subqueries: joining
   * packages and experts would multiply the rows of one by the other. The name's column is
   * ICU-collated, so the database sorts alphabetically.
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

  /** The offering's packages as managed entities, for the adapter to read and to write. */
  @Query(
      "select p from ConsultationPackageEntity p where p.consultationOfferingId = :consultationOfferingId")
  List<ConsultationPackageEntity> findPackageEntities(UUID consultationOfferingId);

  /** Whether the offering has a case (D-23: the offering adapter may read the case table). */
  @Query(
      """
      select exists (
          select c.id from ConsultationCaseEntity c where c.consultationOfferingId = :consultationOfferingId)
      """)
  boolean existsCase(UUID consultationOfferingId);

  /** The offering's eligible experts as managed entities, for the adapter to read and to write. */
  @Query(
      "select e from EligibleExpertEntity e where e.consultationOfferingId = :consultationOfferingId")
  List<EligibleExpertEntity> findEligibleExpertEntities(UUID consultationOfferingId);
}
