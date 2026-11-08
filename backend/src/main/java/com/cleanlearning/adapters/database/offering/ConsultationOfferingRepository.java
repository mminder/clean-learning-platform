package com.cleanlearning.adapters.database.offering;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import com.cleanlearning.adapters.database.offering.entity.OfferingEntity;
import com.cleanlearning.adapters.database.offering.row.ConsultationOfferingSummaryRow;

interface ConsultationOfferingRepository extends Repository<OfferingEntity, UUID> {

    /**
     * Every consultation offering in every status, in one query. The counts are subqueries: joining packages and
     * experts would multiply the rows of one by the other. The name's column is ICU-collated, so the database sorts
     * alphabetically.
     */
    @Query("""
            select new com.cleanlearning.adapters.database.offering.row.ConsultationOfferingSummaryRow(
                o.id, o.name, o.status,
                (select count(p) from ConsultationPackageEntity p where p.consultationOfferingId = o.id),
                (select count(e) from EligibleExpertEntity e where e.consultationOfferingId = o.id))
            from OfferingEntity o
            join ConsultationOfferingEntity c on c.offeringId = o.id
            order by o.name, o.id
            """)
    List<ConsultationOfferingSummaryRow> findSummaries();
}
