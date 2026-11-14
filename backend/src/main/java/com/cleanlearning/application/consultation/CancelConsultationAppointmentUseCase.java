package com.cleanlearning.application.consultation;

import com.cleanlearning.application.consultation.port.LoadCustomerConsultationCasePort;
import com.cleanlearning.application.consultation.port.LockCaseForCancellationPort;
import com.cleanlearning.application.consultation.port.SaveAppointmentCancellationPort;
import com.cleanlearning.domain.consultation.CaseForCancellation;
import com.cleanlearning.domain.consultation.outcome.AppointmentCancellation;
import com.cleanlearning.domain.consultation.view.CustomerConsultationCase;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.user.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Locks only the case (D-27). The expert isn't locked: a cancellation only frees time, and a
 * booking with the expert that doesn't see it yet is still valid.
 */
@Service
public class CancelConsultationAppointmentUseCase {

  private final LockCaseForCancellationPort lockCase;

  private final SaveAppointmentCancellationPort saveCancellation;

  private final LoadCustomerConsultationCasePort loadCase;

  private final Clock clock;

  CancelConsultationAppointmentUseCase(
      LockCaseForCancellationPort lockCase,
      SaveAppointmentCancellationPort saveCancellation,
      LoadCustomerConsultationCasePort loadCase,
      Clock clock) {
    this.lockCase = lockCase;
    this.saveCancellation = saveCancellation;
    this.loadCase = loadCase;
    this.clock = clock;
  }

  @Transactional
  public CustomerConsultationCase invoke(
      CurrentUser user, CancelConsultationAppointmentCommand command) {
    CaseForCancellation consultationCase =
        lockCase
            .lock(command.consultationCaseId(), user.id())
            .orElseThrow(() ->
                new NotFoundException(
                    "Case " + command.consultationCaseId().value() + " not found."));
    // The time after waiting for the lock.
    Instant now = clock.instant();
    AppointmentCancellation cancellation =
        consultationCase.cancelAppointment(command.appointmentId(), command.creditRefunded(), now);
    saveCancellation.save(cancellation);
    // The query flushes the change first.
    return loadCase
        .load(command.consultationCaseId(), user.id(), now)
        .orElseThrow(() -> new IllegalStateException("The locked case is gone."));
  }
}
