package com.cleanlearning.application.consultation;

import com.cleanlearning.application.consultation.port.LoadCustomerConsultationCasePort;
import com.cleanlearning.application.consultation.port.LockCaseForCancellationPort;
import com.cleanlearning.application.consultation.port.SaveCaseCancellationPort;
import com.cleanlearning.domain.consultation.CaseForCancellation;
import com.cleanlearning.domain.consultation.outcome.CaseCancellation;
import com.cleanlearning.domain.consultation.view.CustomerConsultationCase;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.user.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-08 steps 3–5: the customer cancels their case, and with it its appointments that haven't
 * started. It locks only the case (D-27): under the lock, nobody else cancels or books on this
 * case, so the cancellation sees every booking that came before it, and a booking that comes after
 * sees the case cancelled. The experts aren't locked: a cancellation only frees time.
 */
@Service
public class CancelConsultationCaseUseCase {

  private final LockCaseForCancellationPort lockCase;

  private final SaveCaseCancellationPort saveCancellation;

  private final LoadCustomerConsultationCasePort loadCase;

  private final Clock clock;

  CancelConsultationCaseUseCase(
      LockCaseForCancellationPort lockCase,
      SaveCaseCancellationPort saveCancellation,
      LoadCustomerConsultationCasePort loadCase,
      Clock clock) {
    this.lockCase = lockCase;
    this.saveCancellation = saveCancellation;
    this.loadCase = loadCase;
    this.clock = clock;
  }

  /**
   * Locks the case, takes the time, lets the domain decide, writes the cancellation, and answers
   * the case as its customer sees it now: cancelled, without the appointments cancelled with it
   * (spec 05, decision 12).
   *
   * @throws NotFoundException if there's no case with this id, or it's another customer's
   * @throws BusinessRuleException {@code CASE_CANCELLED}, {@code CASE_COMPLETED} or {@code
   *     REFUND_ELIGIBILITY_CHANGED}, as {@code CaseForCancellation} decides
   */
  @Transactional
  public CustomerConsultationCase invoke(CurrentUser user, CancelConsultationCaseCommand command) {
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
    CaseCancellation cancellation = consultationCase.cancel(command.refundEligible(), now);
    // 4. Write.
    saveCancellation.save(cancellation);
    // 5. Answer: the query flushes the changes first.
    return loadCase
        .load(command.consultationCaseId(), user.id(), now)
        .orElseThrow(() -> new IllegalStateException("The locked case is gone."));
  }
}
