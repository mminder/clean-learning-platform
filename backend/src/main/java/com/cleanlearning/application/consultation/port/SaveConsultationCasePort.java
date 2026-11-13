package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.ConsultationCase;

public interface SaveConsultationCasePort {

  /** Inserts a new case. */
  void create(ConsultationCase consultationCase);
}
