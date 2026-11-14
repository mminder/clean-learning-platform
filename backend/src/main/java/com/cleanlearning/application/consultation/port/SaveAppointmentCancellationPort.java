package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.outcome.AppointmentCancellation;

public interface SaveAppointmentCancellationPort {

  void save(AppointmentCancellation cancellation);
}
