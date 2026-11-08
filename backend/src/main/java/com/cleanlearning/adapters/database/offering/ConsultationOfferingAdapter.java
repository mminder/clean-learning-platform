package com.cleanlearning.adapters.database.offering;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.cleanlearning.adapters.database.offering.entity.ConsultationOfferingEntity;
import com.cleanlearning.adapters.database.offering.entity.OfferingEntity;
import com.cleanlearning.adapters.database.offering.row.ConsultationOfferingSummaryRow;
import com.cleanlearning.application.offering.LoadConsultationOfferingPort;
import com.cleanlearning.application.offering.LoadConsultationOfferingSummariesPort;
import com.cleanlearning.application.offering.LockConsultationOfferingPort;
import com.cleanlearning.application.offering.SaveConsultationOfferingPort;
import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.ConsultationOfferingSummary;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.OfferingType;
import com.cleanlearning.domain.offering.value.SessionDuration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

/**
 * The admin's side of the offerings (UC-16). The shop's side is {@code ShopOfferingAdapter}.
 * <p>
 * Writes go through the {@link EntityManager}: the application sets the ids, so a new row is persisted, not merged
 * (Spring Data's {@code save()} would select it first), and a changed row is written by changing its managed entity.
 */
@Component
class ConsultationOfferingAdapter implements LoadConsultationOfferingSummariesPort, LoadConsultationOfferingPort,
        LockConsultationOfferingPort, SaveConsultationOfferingPort {

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

    @Override
    public void create(ConsultationOffering offering) {
        UUID id = offering.id().value();
        // offering first: consultation_offering references it.
        entityManager.persist(new OfferingEntity(id, OfferingType.CONSULTATION, offering.name().value(),
                offering.description(), offering.status()));
        entityManager.persist(new ConsultationOfferingEntity(id, offering.sessionDuration().minutes(),
                offering.cancellationDeadline().hours()));
    }

    /** Changes the managed entities, which the transaction writes when it commits. */
    @Override
    public void update(ConsultationOffering offering) {
        UUID id = offering.id().value();
        OfferingEntity offeringEntity = existing(OfferingEntity.class, id);
        offeringEntity.setName(offering.name().value());
        offeringEntity.setDescription(offering.description());
        offeringEntity.setStatus(offering.status());
        ConsultationOfferingEntity consultationOfferingEntity = existing(ConsultationOfferingEntity.class, id);
        consultationOfferingEntity.setSessionDurationMinutes(offering.sessionDuration().minutes());
        consultationOfferingEntity.setCancellationDeadlineHours(offering.cancellationDeadline().hours());
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

    private static ConsultationOffering toConsultationOffering(OfferingEntity offering,
            ConsultationOfferingEntity consultationOffering) {
        return new ConsultationOffering(new OfferingId(offering.getId()), new OfferingName(offering.getName()),
                offering.getDescription(), new SessionDuration(consultationOffering.getSessionDurationMinutes()),
                new CancellationDeadline(consultationOffering.getCancellationDeadlineHours()), offering.getStatus());
    }

    private static ConsultationOfferingSummary toSummary(ConsultationOfferingSummaryRow row) {
        return new ConsultationOfferingSummary(new OfferingId(row.id()), new OfferingName(row.name()), row.status(),
                Math.toIntExact(row.packageCount()), Math.toIntExact(row.eligibleExpertCount()));
    }
}
