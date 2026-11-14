package com.cleanlearning.adapters.database.availability;

import com.cleanlearning.adapters.database.availability.entity.AvailabilityDateEntity;
import com.cleanlearning.adapters.database.availability.entity.AvailabilityTimeRangeEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

interface AvailabilityRepository
    extends Repository<AvailabilityDateEntity, AvailabilityDateEntity.Key> {

  @Query(
      """
      select d.availableOn from AvailabilityDateEntity d
      where d.expertId = :expertId and d.availableOn >= :from
      order by d.availableOn
      """)
  List<LocalDate> findDates(UUID expertId, LocalDate from);

  @Query(
      """
      select r from AvailabilityTimeRangeEntity r
      where r.expertId = :expertId
      order by r.startTime
      """)
  List<AvailabilityTimeRangeEntity> findTimeRanges(UUID expertId);

  /** A bulk delete: it runs at once, not when the transaction flushes. */
  @Modifying
  @Query(
      "delete from AvailabilityDateEntity d where d.expertId = :expertId and d.availableOn >= :from")
  void deleteDatesFrom(UUID expertId, LocalDate from);

  /** A bulk delete: it runs at once, not when the transaction flushes. */
  @Modifying
  @Query("delete from AvailabilityTimeRangeEntity r where r.expertId = :expertId")
  void deleteTimeRanges(UUID expertId);
}
