package com.cleanlearning.adapters.database.availability.entity;

import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Written and read by {@code AvailabilityAdapter}. The database sets {@code created_at} and {@code updated_at}. An
 * expert's ranges never overlap, so their starts are unique.
 */
@Entity
@Table(name = "availability_time_range")
@IdClass(AvailabilityTimeRangeEntity.Key.class)
public class AvailabilityTimeRangeEntity {

    @Id
    private UUID expertId;

    @Id
    private LocalTime startTime;

    private LocalTime endTime;

    protected AvailabilityTimeRangeEntity() {
        // for JPA
    }

    public AvailabilityTimeRangeEntity(UUID expertId, LocalTime startTime, LocalTime endTime) {
        this.expertId = expertId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    record Key(UUID expertId, LocalTime startTime) {
    }
}
