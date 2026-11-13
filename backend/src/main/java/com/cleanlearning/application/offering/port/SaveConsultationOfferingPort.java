package com.cleanlearning.application.offering.port;

import com.cleanlearning.domain.offering.ConsultationOffering;

public interface SaveConsultationOfferingPort {

  /** Inserts a new consultation offering. */
  void create(ConsultationOffering offering);

  /** Writes the changes of an existing consultation offering. */
  void update(ConsultationOffering offering);
}
