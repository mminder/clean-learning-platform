package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.change.AppointmentCancellation;

public interface SaveAppointmentCancellationPort {

  /**
   * Sets the appointment to {@code CANCELLED}, with whether its credit was refunded. Its case was
   * locked in the same transaction ({@link LockCaseForCancellationPort}), which loaded the
   * appointment.
   */
  void save(AppointmentCancellation cancellation);
}
