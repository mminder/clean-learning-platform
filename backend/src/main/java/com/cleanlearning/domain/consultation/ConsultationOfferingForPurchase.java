package com.cleanlearning.domain.consultation;

import com.cleanlearning.domain.consultation.outcome.NewConsultationCase;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.offering.ConsultationPackage;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingStatus;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.value.Money;
import com.cleanlearning.domain.user.value.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ConsultationOfferingForPurchase(
    OfferingId id,
    OfferingStatus status,
    SessionDuration sessionDuration,
    CancellationDeadline cancellationDeadline,
    List<ConsultationPackage> packages) {

  public static final String OFFERING_NOT_AVAILABLE = "OFFERING_NOT_AVAILABLE";

  public static final String PACKAGE_CHANGED = "PACKAGE_CHANGED";

  public ConsultationOfferingForPurchase {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(sessionDuration, "sessionDuration");
    Objects.requireNonNull(cancellationDeadline, "cancellationDeadline");
    packages = List.copyOf(packages);
  }

  /**
   * A published offering without packages isn't in the shop, so buying from it is {@link
   * #PACKAGE_CHANGED}.
   */
  public NewConsultationCase purchase(
      UserId customerId,
      ConsultationPackageId packageId,
      Money confirmedPrice,
      SessionCount confirmedSessionCount,
      Instant now) {
    if (status != OfferingStatus.PUBLISHED) {
      throw new BusinessRuleException(
          OFFERING_NOT_AVAILABLE, "This offering is no longer available.");
    }
    ConsultationPackage bought =
        packages.stream()
            .filter(consultationPackage -> consultationPackage.id().equals(packageId))
            .filter(consultationPackage -> consultationPackage.price().equals(confirmedPrice))
            .filter(consultationPackage ->
                consultationPackage.sessionCount().equals(confirmedSessionCount))
            .findFirst()
            .orElseThrow(() ->
                new BusinessRuleException(
                    PACKAGE_CHANGED,
                    "This package has changed or is no longer available. Please choose again."));
    return new NewConsultationCase(
        new ConsultationCaseId(UUID.randomUUID()),
        customerId,
        id,
        new PurchaseTerms(
            bought.price(), bought.sessionCount(), sessionDuration, cancellationDeadline),
        now);
  }
}
