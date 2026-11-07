package com.cleanlearning.api.shop;

import java.util.UUID;

import com.cleanlearning.api.MoneyResponse;
import com.cleanlearning.domain.offering.ConsultationPackage;

public record ConsultationPackageResponse(UUID id, int sessionCount, MoneyResponse price) {

    static ConsultationPackageResponse from(ConsultationPackage consultationPackage) {
        return new ConsultationPackageResponse(consultationPackage.id().value(), consultationPackage.sessionCount(),
                MoneyResponse.from(consultationPackage.price()));
    }
}
