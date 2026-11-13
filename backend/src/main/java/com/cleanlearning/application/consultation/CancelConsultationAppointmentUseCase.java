package com.cleanlearning.application.consultation;

import com.cleanlearning.application.consultation.port.LoadCustomerConsultationCasePort;
import com.cleanlearning.application.consultation.port.LockCaseForCancellationPort;
import com.cleanlearning.application.consultation.port.SaveAppointmentCancellationPort;
import com.cleanlearning.domain.consultation.AppointmentCancellation;
import com.cleanlearning.domain.consultation.CaseForCancellation;
import com.cleanlearning.domain.consultation.CustomerConsultationCase;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.user.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-07 steps 3–5: the customer cancels an appointment of their case. It locks only the case
 * (D-27): under the lock, nobody else cancels or books on this case, so every rule is checked again
 * on what's current. The expert isn't locked: a cancellation only frees time, and a booking with
 * the expert that doesn't see it yet is still valid.
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

  /**
   * Locks the case, takes the time, lets the domain decide, writes the cancellation, and answers
   * the case as its customer sees it now: without the appointment, and with the credit back if it
   * was refunded (spec 05, decision 12).
   *
   * @throws NotFoundException if there's no case with this id, it's another customer's, or the
   *     appointment is none of the case's
   * @throws BusinessRuleException {@code APPOINTMENT_CANCELLED}, {@code APPOINTMENT_STARTED} or
   *     {@code DEADLINE_PASSED}, as {@code CaseForCancellation} decides
   */
  @Transactional
  public CustomerConsultationCase invoke(
      CurrentUser user, CancelConsultationAppointmentCommand command) {
    // 1. The case (D-27), scoped to the customer: a second cancellation, or a booking, of it waits
    // here.
    CaseForCancellation consultationCase =
        lockCase
            .lockForCancellation(command.consultationCaseId(), user.id())
            .orElseThrow(() ->
                new NotFoundException(
                    "Case " + command.consultationCaseId().value() + " not found."));
    // 2. The time after waiting for the lock.
    Instant now = clock.instant();
    // 3. The domain decides, checking every rule again.
    AppointmentCancellation cancellation =
        consultationCase.cancelAppointment(command.appointmentId(), command.creditRefunded(), now);
    // 4. Write.
    saveCancellation.save(cancellation);
    // 5. Answer: the query flushes the change first.
    return loadCase
        .load(command.consultationCaseId(), user.id(), now)
        .orElseThrow(() -> new IllegalStateException("The locked case is gone."));
  }
}
