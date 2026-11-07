package com.cleanlearning.adapters.database.offering;

import java.util.List;
import java.util.Optional;
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

    @Query("""
            select new com.cleanlearning.adapters.database.offering.ShopConsultationOfferingRow(
                o.id, o.name, o.description, c.sessionDurationMinutes, c.cancellationDeadlineHours)
            from OfferingEntity o
            join ConsultationOfferingEntity c on c.offeringId = o.id
            where o.id = :id and o.status = 'PUBLISHED'
            """)
    Optional<ShopConsultationOfferingRow> findPublishedConsultationOffering(UUID id);

    @Query("""
            select new com.cleanlearning.adapters.database.offering.ConsultationPackageRow(
                p.id, p.sessionCount, p.price)
            from ConsultationPackageEntity p
            where p.consultationOfferingId = :consultationOfferingId
            order by p.sessionCount, p.id
            """)
    List<ConsultationPackageRow> findPackages(UUID consultationOfferingId);

    /**
     * The eligible experts with their profiles, sorted by display name (D-23: the offering adapter may read the user
     * tables).
     */
    @Query("""
            select new com.cleanlearning.adapters.database.offering.EligibleExpertRow(
                p.userId, p.displayName, p.bio)
            from EligibleExpertEntity e
            join ExpertProfileEntity p on p.userId = e.expertId
            where e.consultationOfferingId = :consultationOfferingId
            order by p.displayName, p.userId
            """)
    List<EligibleExpertRow> findEligibleExperts(UUID consultationOfferingId);
}
