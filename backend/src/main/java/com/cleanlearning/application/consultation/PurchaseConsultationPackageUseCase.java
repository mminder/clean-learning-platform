package com.cleanlearning.application.consultation;

import com.cleanlearning.application.consultation.port.LoadCustomerConsultationCasePort;
import com.cleanlearning.application.consultation.port.LockConsultationOfferingForPurchasePort;
import com.cleanlearning.application.consultation.port.SaveConsultationCasePort;
import com.cleanlearning.domain.consultation.ConsultationOfferingForPurchase;
import com.cleanlearning.domain.consultation.change.NewConsultationCase;
import com.cleanlearning.domain.consultation.view.CustomerConsultationCase;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.user.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC-03 steps 5–6: the customer buys a package of a consultation offering and gets a case. */
@Service
public class PurchaseConsultationPackageUseCase {

  private final LockConsultationOfferingForPurchasePort lockOffering;

  private final SaveConsultationCasePort saveCase;

  private final LoadCustomerConsultationCasePort loadCase;

  private final Clock clock;

  PurchaseConsultationPackageUseCase(
      LockConsultationOfferingForPurchasePort lockOffering,
      SaveConsultationCasePort saveCase,
      LoadCustomerConsultationCasePort loadCase,
      Clock clock) {
    this.lockOffering = lockOffering;
    this.saveCase = saveCase;
    this.loadCase = loadCase;
    this.clock = clock;
  }

  /**
   * Locks the offering first (D-27), so an unpublish waits for the purchase or the purchase for it.
   * The current user is the case's customer. Answers the new case as its customer sees it.
   *
   * @throws NotFoundException if there's no consultation offering with this id
   * @throws BusinessRuleException if the offering isn't published ({@code OFFERING_NOT_AVAILABLE}),
   *     or the package isn't the offering's or its price or session count isn't the confirmed one
   *     ({@code PACKAGE_CHANGED})
   */
  @Transactional
  public CustomerConsultationCase invoke(
      CurrentUser user, PurchaseConsultationPackageCommand command) {
    ConsultationOfferingForPurchase offering =
        lockOffering
            .lock(command.offeringId())
            .orElseThrow(() ->
                new NotFoundException("Offering " + command.offeringId().value() + " not found."));
    Instant now = clock.instant();
    NewConsultationCase consultationCase =
        offering.purchase(
            user.id(), command.packageId(), command.price(), command.sessionCount(), now);
    saveCase.create(consultationCase);
    return loadCase
        .load(consultationCase.id(), user.id(), now)
        .orElseThrow(() ->
            new IllegalStateException(
                "Case " + consultationCase.id().value() + " was created but isn't found."));
  }
}
