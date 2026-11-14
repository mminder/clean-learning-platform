package com.cleanlearning.adapters.database.availability;

import com.cleanlearning.adapters.database.availability.entity.AvailabilityDateEntity;
import com.cleanlearning.adapters.database.availability.entity.AvailabilityTimeRangeEntity;
import com.cleanlearning.application.availability.port.LoadAvailabilityPort;
import com.cleanlearning.application.availability.port.ReplaceAvailabilityPort;
import com.cleanlearning.domain.availability.Availability;
import com.cleanlearning.domain.availability.value.TimeRange;
import com.cleanlearning.domain.user.value.UserId;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.stereotype.Component;

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
    return new Availability(
        new TreeSet<>(repository.findDates(expertId.value(), from)),
        repository.findTimeRanges(expertId.value()).stream()
            .map(entity -> new TimeRange(entity.getStartTime(), entity.getEndTime()))
            .toList());
  }

  /**
   * Bulk deletes, then persists the new rows: Hibernate flushes inserts before deletes, so removing
   * entities and persisting rows with the same keys in one flush would fail with a duplicate key; a
   * bulk delete runs at once. The availability is replaced as a whole, so there is no difference to
   * write.
   */
  @Override
  public void replaceFrom(UserId expertId, LocalDate from, Availability availability) {
    UUID id = expertId.value();
    repository.deleteDatesFrom(id, from);
    repository.deleteTimeRanges(id);
    availability
        .dates()
        .forEach(date -> entityManager.persist(new AvailabilityDateEntity(id, date)));
    availability
        .timeRanges()
        .forEach(range ->
            entityManager.persist(new AvailabilityTimeRangeEntity(id, range.start(), range.end())));
  }
}
