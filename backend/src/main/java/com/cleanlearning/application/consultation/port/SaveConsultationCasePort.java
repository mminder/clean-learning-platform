package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.outcome.NewConsultationCase;

public interface SaveConsultationCasePort {

  void create(NewConsultationCase consultationCase);
}
