package com.cleanlearning.adapters.database.offering;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cleanlearning.adapters.database.offering.entity.ConsultationOfferingEntity;
import com.cleanlearning.adapters.database.offering.entity.ConsultationPackageEntity;
import com.cleanlearning.adapters.database.offering.entity.EligibleExpertEntity;
import com.cleanlearning.adapters.database.offering.entity.OfferingEntity;
import com.cleanlearning.adapters.database.offering.row.ConsultationOfferingSummaryRow;
import com.cleanlearning.application.offering.port.CheckOfferingPurchasesPort;
import com.cleanlearning.application.offering.port.DeleteConsultationOfferingPort;
import com.cleanlearning.application.offering.port.LoadConsultationOfferingPort;
import com.cleanlearning.application.offering.port.LoadConsultationOfferingSummariesPort;
import com.cleanlearning.application.offering.port.LockConsultationOfferingPort;
import com.cleanlearning.application.offering.port.SaveConsultationOfferingPort;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.ConsultationOfferingSummary;
import com.cleanlearning.domain.offering.ConsultationPackage;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.OfferingType;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.value.Money;
import com.cleanlearning.domain.user.value.UserId;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

/**
 * The admin's side of the offerings (UC-16). The shop's side is {@code ShopOfferingAdapter}. It reads the case table
 * for the purchase guard (D-23).
 * <p>
 * Writes go through the {@link EntityManager}: the application sets the ids, so a new row is persisted, not merged
 * (Spring Data's {@code save()} would select it first), and a changed row is written by changing its managed entity.
 */
@Component
class ConsultationOfferingAdapter implements LoadConsultationOfferingSummariesPort, LoadConsultationOfferingPort,
        LockConsultationOfferingPort, SaveConsultationOfferingPort, DeleteConsultationOfferingPort,
        CheckOfferingPurchasesPort {

    private final ConsultationOfferingRepository repository;

    private final EntityManager entityManager;

    ConsultationOfferingAdapter(ConsultationOfferingRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public List<ConsultationOfferingSummary> loadSummaries() {
        return repository.findSummaries().stream().map(ConsultationOfferingAdapter::toSummary).toList();
    }

    @Override
    public Optional<ConsultationOffering> load(OfferingId id) {
        return Optional.ofNullable(entityManager.find(OfferingEntity.class, id.value()))
                .flatMap(this::withConsultationOffering);
    }

    /**
     * {@code PESSIMISTIC_WRITE} on the {@code offering} row, which Hibernate sends to Postgres as
     * {@code select … for no key update}; then its {@code consultation_offering} row.
     */
    @Override
    public Optional<ConsultationOffering> lock(OfferingId id) {
        return Optional.ofNullable(entityManager.find(OfferingEntity.class, id.value(), LockModeType.PESSIMISTIC_WRITE))
                .flatMap(this::withConsultationOffering);
    }

    /** An {@code exists} over {@code consultation_case}: a purchase is a case. */
    @Override
    public boolean hasPurchases(OfferingId id) {
        return repository.existsCase(id.value());
    }

    @Override
    public void create(ConsultationOffering offering) {
        UUID id = offering.id().value();
        // offering first: consultation_offering references it.
        // A new offering has no image yet: it gets one after its first save.
        entityManager.persist(new OfferingEntity(id, OfferingType.CONSULTATION, offering.name().value(),
                offering.description(), offering.status()));
        entityManager.persist(new ConsultationOfferingEntity(id, offering.sessionDuration().minutes(),
                offering.cancellationDeadline().hours()));
        offering.packages().forEach(consultationPackage -> entityManager.persist(newEntity(id, consultationPackage)));
        offering.eligibleExpertIds().forEach(expertId -> entityManager.persist(
                new EligibleExpertEntity(id, expertId.value())));
    }

    /** Changes the managed entities, which the transaction writes when it commits. */
    @Override
    public void update(ConsultationOffering offering) {
        UUID id = offering.id().value();
        OfferingEntity offeringEntity = existing(OfferingEntity.class, id);
        offeringEntity.setName(offering.name().value());
        offeringEntity.setDescription(offering.description());
        offeringEntity.setStatus(offering.status());
        offeringEntity.setImageId(offering.imageId().map(ImageId::value).orElse(null));
        ConsultationOfferingEntity consultationOfferingEntity = existing(ConsultationOfferingEntity.class, id);
        consultationOfferingEntity.setSessionDurationMinutes(offering.sessionDuration().minutes());
        consultationOfferingEntity.setCancellationDeadlineHours(offering.cancellationDeadline().hours());
        writePackages(id, offering.packages());
        writeEligibleExperts(id, offering.eligibleExpertIds());
    }

    /**
     * Removes the managed entities in the order the foreign keys need: the eligible experts, the packages, the
     * {@code consultation_offering} row and then the {@code offering} row. The transaction deletes them in this order
     * when it commits. The use case deletes the offering's image after them.
     */
    @Override
    public void delete(OfferingId id) {
        UUID offeringId = id.value();
        repository.findEligibleExpertEntities(offeringId).forEach(entityManager::remove);
        repository.findPackageEntities(offeringId).forEach(entityManager::remove);
        entityManager.remove(existing(ConsultationOfferingEntity.class, offeringId));
        entityManager.remove(existing(OfferingEntity.class, offeringId));
    }

    /**
     * Writes only the difference to the loaded packages: deletes the dropped ones, updates the kept ones (the
     * transaction writes a row only if a value changed) and inserts the new ones. A kept package is never deleted
     * and inserted again.
     */
    private void writePackages(UUID offeringId, List<ConsultationPackage> packages) {
        Map<UUID, ConsultationPackageEntity> loaded = repository.findPackageEntities(offeringId).stream()
                .collect(Collectors.toMap(ConsultationPackageEntity::getId, Function.identity()));
        for (ConsultationPackage consultationPackage : packages) {
            ConsultationPackageEntity entity = loaded.remove(consultationPackage.id().value());
            if (entity == null) {
                entityManager.persist(newEntity(offeringId, consultationPackage));
            } else {
                entity.setSessionCount(consultationPackage.sessionCount().value());
                entity.setPrice(consultationPackage.price().amount());
            }
        }
        loaded.values().forEach(entityManager::remove);
    }

    /**
     * Writes only the difference to the loaded eligible experts, like the packages: deletes the dropped rows and
     * inserts the new ones. A row is never deleted and inserted again in one transaction, which would insert first.
     */
    private void writeEligibleExperts(UUID offeringId, Set<UserId> eligibleExpertIds) {
        Set<UUID> wanted = eligibleExpertIds.stream().map(UserId::value).collect(Collectors.toSet());
        Set<UUID> loaded = new HashSet<>();
        for (EligibleExpertEntity entity : repository.findEligibleExpertEntities(offeringId)) {
            if (wanted.contains(entity.getExpertId())) {
                loaded.add(entity.getExpertId());
            } else {
                entityManager.remove(entity);
            }
        }
        wanted.stream().filter(expertId -> !loaded.contains(expertId))
                .forEach(expertId -> entityManager.persist(new EligibleExpertEntity(offeringId, expertId)));
    }

    private static ConsultationPackageEntity newEntity(UUID offeringId, ConsultationPackage consultationPackage) {
        return new ConsultationPackageEntity(consultationPackage.id().value(), offeringId,
                consultationPackage.sessionCount().value(), consultationPackage.price().amount());
    }

    /** A training offering (UC-17) has no {@code consultation_offering} row, so it isn't found here. */
    private Optional<ConsultationOffering> withConsultationOffering(OfferingEntity offering) {
        return Optional.ofNullable(entityManager.find(ConsultationOfferingEntity.class, offering.getId()))
                .map(consultationOffering -> toConsultationOffering(offering, consultationOffering));
    }

    private <T> T existing(Class<T> entityClass, UUID id) {
        T entity = entityManager.find(entityClass, id);
        if (entity == null) {
            throw new IllegalStateException(entityClass.getSimpleName() + " " + id + " doesn't exist.");
        }
        return entity;
    }

    private ConsultationOffering toConsultationOffering(OfferingEntity offering,
            ConsultationOfferingEntity consultationOffering) {
        List<ConsultationPackage> packages = repository.findPackageEntities(offering.getId()).stream()
                .map(ConsultationOfferingAdapter::toConsultationPackage).toList();
        Set<UserId> eligibleExpertIds = repository.findEligibleExpertEntities(offering.getId()).stream()
                .map(entity -> new UserId(entity.getExpertId())).collect(Collectors.toSet());
        return new ConsultationOffering(new OfferingId(offering.getId()), new OfferingName(offering.getName()),
                offering.getDescription(), new SessionDuration(consultationOffering.getSessionDurationMinutes()),
                new CancellationDeadline(consultationOffering.getCancellationDeadlineHours()), offering.getStatus(),
                packages, eligibleExpertIds, Optional.ofNullable(offering.getImageId()).map(ImageId::new));
    }

    private static ConsultationPackage toConsultationPackage(ConsultationPackageEntity entity) {
        return new ConsultationPackage(new ConsultationPackageId(entity.getId()),
                new SessionCount(entity.getSessionCount()), new Money(entity.getPrice()));
    }

    private static ConsultationOfferingSummary toSummary(ConsultationOfferingSummaryRow row) {
        return new ConsultationOfferingSummary(new OfferingId(row.id()), new OfferingName(row.name()), row.status(),
                Math.toIntExact(row.packageCount()), Math.toIntExact(row.eligibleExpertCount()));
    }
}
