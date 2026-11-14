package com.cleanlearning.application.offering.port;

import com.cleanlearning.domain.offering.ConsultationOffering;

public interface SaveConsultationOfferingPort {

  void create(ConsultationOffering offering);

  void update(ConsultationOffering offering);
}
