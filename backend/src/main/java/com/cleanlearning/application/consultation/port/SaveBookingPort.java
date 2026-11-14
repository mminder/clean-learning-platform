package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.change.Booking;

public interface SaveBookingPort {

  /**
   * Inserts the booking's appointment as {@code BOOKED}, and on the first booking sets the case's
   * assigned expert. The case was locked in the same transaction ({@link LockCaseForBookingPort}).
   */
  void save(Booking booking);
}
