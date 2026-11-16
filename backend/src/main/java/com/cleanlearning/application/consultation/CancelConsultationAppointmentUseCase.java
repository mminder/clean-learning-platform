package com.cleanlearning.application.consultation;

import com.cleanlearning.application.consultation.port.LoadCustomerConsultationCasePort;
import com.cleanlearning.application.consultation.port.LockConsultationCasePort;
import com.cleanlearning.application.consultation.port.SaveAppointmentCancellationPort;
import com.cleanlearning.domain.consultation.ConsultationCase;
import com.cleanlearning.domain.consultation.outcome.AppointmentCancellation;
import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.view.CustomerConsultationCase;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.user.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Locks only the case (D-27). The expert isn't locked: a cancellation only frees time, and a
 * booking with the expert that doesn't see it yet is still valid.
 */
@Service
public class CancelConsultationAppointmentUseCase {

  private final LockConsultationCasePort lockCase;

  private final SaveAppointmentCancellationPort saveCancellation;

  private final LoadCustomerConsultationCasePort loadCase;

  private final Clock clock;

  CancelConsultationAppointmentUseCase(
      LockConsultationCasePort lockCase,
      SaveAppointmentCancellationPort saveCancellation,
      LoadCustomerConsultationCasePort loadCase,
      Clock clock) {
    this.lockCase = lockCase;
    this.saveCancellation = saveCancellation;
    this.loadCase = loadCase;
    this.clock = clock;
  }

  @Transactional
  public CustomerConsultationCase invoke(CurrentUser user, Command command) {
    ConsultationCase consultationCase =
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

  public record Command(
      ConsultationCaseId consultationCaseId, AppointmentId appointmentId, boolean creditRefunded) {

    public Command {
      Objects.requireNonNull(consultationCaseId, "consultationCaseId");
      Objects.requireNonNull(appointmentId, "appointmentId");
    }
  }
}
