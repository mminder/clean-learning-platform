package com.cleanlearning.adapters.database.consultation.entity;

/**
 * The statuses an appointment's row can hold, the values of the column's check. The domain's {@code AppointmentStatus}
 * is what its customer sees and also has {@code CONSUMED}, which is derived, never stored (D-11). UC-07 adds
 * {@code CANCELLED}.
 */
public enum StoredAppointmentStatus {
    BOOKED
}
