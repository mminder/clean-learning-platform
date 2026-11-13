package com.cleanlearning.domain.consultation.value;

/**
 * An appointment's status as its customer sees it at a given time (glossary): {@code BOOKED} while
 * its start is ahead, {@code CONSUMED} from its start on. Only {@code BOOKED} is stored; {@code
 * CONSUMED} is derived, never stored (D-11). UC-07 adds {@code CANCELLED}.
 */
public enum AppointmentStatus {
  BOOKED,
  CONSUMED
}
