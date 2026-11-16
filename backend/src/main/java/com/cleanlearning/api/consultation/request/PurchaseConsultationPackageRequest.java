package com.cleanlearning.api.consultation.request;

import com.cleanlearning.api.offering.request.ConsultationPackageRequest;
import com.cleanlearning.application.consultation.PurchaseConsultationPackageUseCase;
import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.shared.value.Money;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

/** The session count is an {@code Integer}, so a missing one is a 400, not a silent 0. */
public record PurchaseConsultationPackageRequest(
    @NotNull UUID offeringId,
    @NotNull UUID packageId,
    @NotNull
        @Pattern(
            regexp = ConsultationPackageRequest.PRICE_PATTERN,
            message = ConsultationPackageRequest.PRICE_MESSAGE)
        String price,
    @NotNull @Positive Integer sessionCount) {

  public PurchaseConsultationPackageUseCase.Command toCommand() {
    return new PurchaseConsultationPackageUseCase.Command(
        new OfferingId(offeringId),
        new ConsultationPackageId(packageId),
        Money.chf(price),
        new SessionCount(sessionCount));
  }
}
