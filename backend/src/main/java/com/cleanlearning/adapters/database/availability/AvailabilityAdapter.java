package com.cleanlearning.adapters.database.availability;

import java.time.LocalDate;
import java.util.TreeSet;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.cleanlearning.adapters.database.availability.entity.AvailabilityDateEntity;
import com.cleanlearning.adapters.database.availability.entity.AvailabilityTimeRangeEntity;
import com.cleanlearning.application.availability.port.LoadAvailabilityPort;
import com.cleanlearning.application.availability.port.ReplaceAvailabilityPort;
import com.cleanlearning.domain.availability.Availability;
import com.cleanlearning.domain.availability.value.TimeRange;
import com.cleanlearning.domain.user.value.UserId;

import jakarta.persistence.EntityManager;

/**
 * An expert's availability (UC-13): their dates and their daily time ranges.
 */
@Component
class AvailabilityAdapter implements LoadAvailabilityPort, ReplaceAvailabilityPort {

    private final AvailabilityRepository repository;

    private final EntityManager entityManager;

    AvailabilityAdapter(AvailabilityRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public Availability load(UserId expertId, LocalDate from) {
        return new Availability(new TreeSet<>(repository.findDates(expertId.value(), from)),
                repository.findTimeRanges(expertId.value()).stream()
                        .map(entity -> new TimeRange(entity.getStartTime(), entity.getEndTime())).toList());
    }

    /**
     * Deletes with bulk {@code delete} statements, then persists the new rows. Unlike the offering's packages, the rows
     * aren't written as a difference: the availability is replaced as a whole on every save, and nothing refers to its
     * rows. And Hibernate flushes inserts before deletes, so removing entities and persisting rows with the same keys
     * in one flush would fail with a duplicate key; a bulk delete runs at once.
     */
    @Override
    public void replaceFrom(UserId expertId, LocalDate from, Availability availability) {
        UUID id = expertId.value();
        repository.deleteDatesFrom(id, from);
        repository.deleteTimeRanges(id);
        availability.dates().forEach(date -> entityManager.persist(new AvailabilityDateEntity(id, date)));
        availability.timeRanges().forEach(range -> entityManager.persist(
                new AvailabilityTimeRangeEntity(id, range.start(), range.end())));
    }
}
