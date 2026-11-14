package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.outcome.CaseCancellation;

public interface SaveCaseCancellationPort {

  void save(CaseCancellation cancellation);
}
