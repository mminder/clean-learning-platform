package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.view.CustomerConsultationCase;
import com.cleanlearning.domain.user.value.UserId;
import java.time.Instant;
import java.util.Optional;

public interface LoadCustomerConsultationCasePort {

  Optional<CustomerConsultationCase> load(ConsultationCaseId id, UserId customerId, Instant asOf);
}
