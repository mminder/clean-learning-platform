package com.cleanlearning.api.response;

import com.cleanlearning.domain.offering.ConsultationPackage;
import java.util.UUID;

/** Shared by the shop and the admin, so there is one schema in the OpenAPI document. */
public record ConsultationPackageResponse(UUID id, int sessionCount, MoneyResponse price) {

  public static ConsultationPackageResponse from(ConsultationPackage consultationPackage) {
    return new ConsultationPackageResponse(
        consultationPackage.id().value(),
        consultationPackage.sessionCount().value(),
        MoneyResponse.from(consultationPackage.price()));
  }
}
