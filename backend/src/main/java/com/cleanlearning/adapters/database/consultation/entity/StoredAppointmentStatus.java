package com.cleanlearning.adapters.database.consultation.entity;

/**
 * The statuses an appointment's row can hold, the values of the column's check. The domain's {@code AppointmentStatus}
 * is what its customer sees and has {@code CONSUMED}, which is derived, never stored (D-11), but not {@code CANCELLED}:
 * the customer never sees a cancelled appointment (spec 05, decision 3).
 */
public enum StoredAppointmentStatus {
    BOOKED,
    CANCELLED
}
