package com.cleanlearning.application.consultation;

import com.cleanlearning.application.consultation.port.LoadCustomerConsultationCasePort;
import com.cleanlearning.application.consultation.port.LockConsultationCasePort;
import com.cleanlearning.application.consultation.port.SaveCaseCancellationPort;
import com.cleanlearning.domain.consultation.ConsultationCase;
import com.cleanlearning.domain.consultation.outcome.CaseCancellation;
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
 * Locks only the case (D-27), so the cancellation sees every booking before it, and a booking after
 * it sees the case cancelled. The experts aren't locked: a cancellation only frees time.
 */
@Service
public class CancelConsultationCaseUseCase {

  private final LockConsultationCasePort lockCase;

  private final SaveCaseCancellationPort saveCancellation;

  private final LoadCustomerConsultationCasePort loadCase;

  private final Clock clock;

  CancelConsultationCaseUseCase(
      LockConsultationCasePort lockCase,
      SaveCaseCancellationPort saveCancellation,
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
    CaseCancellation cancellation = consultationCase.cancel(command.refundEligible(), now);
    saveCancellation.save(cancellation);
    // The query flushes the changes first.
    return loadCase
        .load(command.consultationCaseId(), user.id(), now)
        .orElseThrow(() -> new IllegalStateException("The locked case is gone."));
  }

  public record Command(ConsultationCaseId consultationCaseId, boolean refundEligible) {

    public Command {
      Objects.requireNonNull(consultationCaseId, "consultationCaseId");
    }
  }
}
