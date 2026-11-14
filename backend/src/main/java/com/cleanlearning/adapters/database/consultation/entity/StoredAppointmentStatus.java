package com.cleanlearning.adapters.database.consultation.entity;

/**
 * The values of the column's check. The domain's {@code AppointmentStatus} has the derived {@code
 * CONSUMED} but not {@code CANCELLED}: the customer never sees a cancelled appointment.
 */
public enum StoredAppointmentStatus {
  BOOKED,
  CANCELLED
}
