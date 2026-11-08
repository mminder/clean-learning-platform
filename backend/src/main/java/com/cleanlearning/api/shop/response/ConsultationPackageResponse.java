package com.cleanlearning.api.shop.response;

import java.util.UUID;

import com.cleanlearning.api.response.MoneyResponse;
import com.cleanlearning.domain.offering.ConsultationPackage;

public record ConsultationPackageResponse(UUID id, int sessionCount, MoneyResponse price) {

    public static ConsultationPackageResponse from(ConsultationPackage consultationPackage) {
        return new ConsultationPackageResponse(consultationPackage.id().value(),
                consultationPackage.sessionCount().value(), MoneyResponse.from(consultationPackage.price()));
    }
}
