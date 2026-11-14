package com.cleanlearning.domain.consultation;

import com.cleanlearning.domain.consultation.change.NewConsultationCase;
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

/**
 * A consultation offering as a purchase needs it (UC-03): its status, the terms it copies into a
 * case, and its packages. It holds UC-03's rules: only a published offering is bought, and only at
 * the price and session count the customer confirmed.
 */
public record ConsultationOfferingForPurchase(
    OfferingId id,
    OfferingStatus status,
    SessionDuration sessionDuration,
    CancellationDeadline cancellationDeadline,
    List<ConsultationPackage> packages) {

  /** The offering was unpublished or archived while the customer chose. */
  public static final String OFFERING_NOT_AVAILABLE = "OFFERING_NOT_AVAILABLE";

  /** The package was removed, or its price or session count changed, while the customer chose. */
  public static final String PACKAGE_CHANGED = "PACKAGE_CHANGED";

  public ConsultationOfferingForPurchase {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(sessionDuration, "sessionDuration");
    Objects.requireNonNull(cancellationDeadline, "cancellationDeadline");
    packages = List.copyOf(packages);
  }

  /**
   * The customer buys the package (UC-03 steps 5–6): a new case with a new id, the package's price
   * and session count, the offering's session duration and cancellation deadline, and {@code now}
   * as the purchase time. The status is checked first. A published offering without packages isn't
   * in the shop (UC-02), and buying from it is {@link #PACKAGE_CHANGED}.
   *
   * @param confirmedPrice the price the customer confirmed
   * @param confirmedSessionCount the session count the customer confirmed
   * @throws BusinessRuleException {@link #OFFERING_NOT_AVAILABLE} if the offering isn't published,
   *     or {@link #PACKAGE_CHANGED} if the package isn't one of the offering's, or its price or
   *     session count isn't the confirmed one
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
