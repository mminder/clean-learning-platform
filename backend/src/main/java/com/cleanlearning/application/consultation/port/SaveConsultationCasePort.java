package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.outcome.NewConsultationCase;

public interface SaveConsultationCasePort {

  /** Inserts a new case. */
  void create(NewConsultationCase consultationCase);
}
