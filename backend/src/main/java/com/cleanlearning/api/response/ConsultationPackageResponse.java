package com.cleanlearning.api.response;

import java.util.UUID;

import com.cleanlearning.domain.offering.ConsultationPackage;

/**
 * A package of a consultation offering. Shared by the shop and the admin, so there is one schema in the OpenAPI
 * document.
 */
public record ConsultationPackageResponse(UUID id, int sessionCount, MoneyResponse price) {

    public static ConsultationPackageResponse from(ConsultationPackage consultationPackage) {
        return new ConsultationPackageResponse(consultationPackage.id().value(),
                consultationPackage.sessionCount().value(), MoneyResponse.from(consultationPackage.price()));
    }
}
