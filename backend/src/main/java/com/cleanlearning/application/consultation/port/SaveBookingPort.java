package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.outcome.Booking;

public interface SaveBookingPort {

  void save(Booking booking);
}
