package com.cleanlearning.adapters.database.offering;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

interface OfferingRepository extends Repository<OfferingEntity, UUID> {

    /**
     * Every package price of every published offering in one query, sorted by offering name, so the rows of one
     * offering are next to each other.
     */
    @Query("""
            select new com.cleanlearning.adapters.database.offering.ShopOfferingPriceRow(
                o.id, o.type, o.name, o.description, p.price)
            from OfferingEntity o
            join ConsultationPackageEntity p on p.consultationOfferingId = o.id
            where o.status = 'PUBLISHED'
            order by o.name, o.id
            """)
    List<ShopOfferingPriceRow> findPublishedWithPackagePrices();
}
