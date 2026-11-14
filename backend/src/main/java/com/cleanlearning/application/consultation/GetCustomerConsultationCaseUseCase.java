package com.cleanlearning.application.consultation;

import com.cleanlearning.application.consultation.port.LoadCustomerConsultationCasePort;
import com.cleanlearning.domain.consultation.read.CustomerConsultationCase;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.user.CurrentUser;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC-05: the customer opens one of their cases. */
@Service
public class GetCustomerConsultationCaseUseCase {

  private final LoadCustomerConsultationCasePort loadCase;

  private final Clock clock;

  GetCustomerConsultationCaseUseCase(LoadCustomerConsultationCasePort loadCase, Clock clock) {
    this.loadCase = loadCase;
    this.clock = clock;
  }

  /**
   * The case as shown now: its appointments' status and the credits are derived from the current
   * time.
   *
   * @throws NotFoundException if there's no case with this id, or it's another customer's: both
   *     answer 404, so nobody can probe which ids exist
   */
  @Transactional(readOnly = true)
  public CustomerConsultationCase invoke(CurrentUser user, ConsultationCaseId id) {
    return loadCase
        .load(id, user.id(), clock.instant())
        .orElseThrow(() -> new NotFoundException("Case " + id.value() + " not found."));
  }
}
