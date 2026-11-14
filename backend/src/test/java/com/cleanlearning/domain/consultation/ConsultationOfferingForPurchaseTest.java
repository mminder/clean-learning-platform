package com.cleanlearning.domain.consultation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.cleanlearning.domain.consultation.outcome.NewConsultationCase;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * UC-03's rules for a purchase, as the plan's purchase table in
 * _docs/tasks/03-buy-consultation-package/README.md lists them: the status first, then the package,
 * its price and its session count.
 */
class ConsultationOfferingForPurchaseTest {

  private static final OfferingId CAREER_COACHING =
      new OfferingId(UUID.fromString("00000000-0000-0000-0000-000000001001"));

  private static final ConsultationPackage THREE_SESSIONS =
      new ConsultationPackage(
          new ConsultationPackageId(UUID.fromString("00000000-0000-0000-0000-000000001011")),
          new SessionCount(3),
          Money.chf("450.00"));

  private static final ConsultationPackage FIVE_SESSIONS =
      new ConsultationPackage(
          new ConsultationPackageId(UUID.fromString("00000000-0000-0000-0000-000000001012")),
          new SessionCount(5),
          Money.chf("700.00"));

  private static final UserId CUSTOMER_1 =
      new UserId(UUID.fromString("00000000-0000-0000-0000-000000000101"));

  private static final Instant NOW = Instant.parse("2026-11-09T14:03:12.345Z");

  private static final String NOT_AVAILABLE = "This offering is no longer available.";

  private static final String CHANGED =
      "This package has changed or is no longer available. Please choose again.";

  private static ConsultationOfferingForPurchase offering(
      OfferingStatus status, List<ConsultationPackage> packages) {
    return new ConsultationOfferingForPurchase(
        CAREER_COACHING, status, new SessionDuration(60), new CancellationDeadline(24), packages);
  }

  private static ConsultationOfferingForPurchase published() {
    return offering(OfferingStatus.PUBLISHED, List.of(THREE_SESSIONS, FIVE_SESSIONS));
  }

  private static NewConsultationCase buyThreeSessions(ConsultationOfferingForPurchase offering) {
    return offering.purchase(
        CUSTOMER_1, THREE_SESSIONS.id(), Money.chf("450.00"), new SessionCount(3), NOW);
  }

  private static void assertRefused(Runnable purchase, String code, String message) {
    BusinessRuleException refused =
        catchThrowableOfType(BusinessRuleException.class, purchase::run);
    assertThat(refused).as("refused").isNotNull();
    assertThat(refused.code()).isEqualTo(code);
    assertThat(refused).hasMessage(message);
  }

  @Test
  void createsACaseWithTheCopiedTermsForTheCustomer() {
    NewConsultationCase consultationCase = buyThreeSessions(published());

    assertThat(consultationCase.id()).isNotNull();
    assertThat(consultationCase.customerId()).isEqualTo(CUSTOMER_1);
    assertThat(consultationCase.offeringId()).isEqualTo(CAREER_COACHING);
    // the package's price and session count, the offering's session duration and deadline
    assertThat(consultationCase.purchaseTerms())
        .isEqualTo(
            new PurchaseTerms(
                Money.chf("450.00"),
                new SessionCount(3),
                new SessionDuration(60),
                new CancellationDeadline(24)));
    assertThat(consultationCase.purchasedAt()).isEqualTo(NOW);
  }

  @Test
  void createsANewCaseForEachPurchase() {
    ConsultationOfferingForPurchase offering = published();

    assertThat(buyThreeSessions(offering).id()).isNotEqualTo(buyThreeSessions(offering).id());
  }

  @Test
  void buysAnyOfTheOfferingsPackages() {
    NewConsultationCase consultationCase =
        published()
            .purchase(CUSTOMER_1, FIVE_SESSIONS.id(), Money.chf("700"), new SessionCount(5), NOW);

    assertThat(consultationCase.purchaseTerms().pricePaid()).isEqualTo(Money.chf("700.00"));
    assertThat(consultationCase.purchaseTerms().sessionCount()).isEqualTo(new SessionCount(5));
  }

  @ParameterizedTest
  @EnumSource(
      value = OfferingStatus.class,
      names = {"DRAFT", "ARCHIVED"})
  void refusesAnOfferingThatIsNotPublished(OfferingStatus status) {
    ConsultationOfferingForPurchase offering =
        offering(status, List.of(THREE_SESSIONS, FIVE_SESSIONS));

    assertRefused(
        () -> buyThreeSessions(offering),
        ConsultationOfferingForPurchase.OFFERING_NOT_AVAILABLE,
        NOT_AVAILABLE);
  }

  @Test
  void checksTheStatusBeforeThePackage() {
    ConsultationOfferingForPurchase offering =
        offering(OfferingStatus.DRAFT, List.of(FIVE_SESSIONS));

    assertRefused(
        () -> buyThreeSessions(offering),
        ConsultationOfferingForPurchase.OFFERING_NOT_AVAILABLE,
        NOT_AVAILABLE);
  }

  @Test
  void refusesAPackageThatIsNotTheOfferings() {
    ConsultationOfferingForPurchase offering =
        offering(OfferingStatus.PUBLISHED, List.of(FIVE_SESSIONS));

    assertRefused(
        () -> buyThreeSessions(offering), ConsultationOfferingForPurchase.PACKAGE_CHANGED, CHANGED);
  }

  @Test
  void refusesAPublishedOfferingWithoutPackages() {
    ConsultationOfferingForPurchase offering = offering(OfferingStatus.PUBLISHED, List.of());

    assertRefused(
        () -> buyThreeSessions(offering), ConsultationOfferingForPurchase.PACKAGE_CHANGED, CHANGED);
  }

  @Test
  void refusesAPriceOtherThanThePackages() {
    ConsultationOfferingForPurchase offering = published();

    assertRefused(
        () ->
            offering.purchase(
                CUSTOMER_1, THREE_SESSIONS.id(), Money.chf("500.00"), new SessionCount(3), NOW),
        ConsultationOfferingForPurchase.PACKAGE_CHANGED,
        CHANGED);
    assertRefused(
        () ->
            offering.purchase(
                CUSTOMER_1, THREE_SESSIONS.id(), Money.chf("449.99"), new SessionCount(3), NOW),
        ConsultationOfferingForPurchase.PACKAGE_CHANGED,
        CHANGED);
  }

  @Test
  void refusesASessionCountOtherThanThePackages() {
    ConsultationOfferingForPurchase offering = published();

    assertRefused(
        () ->
            offering.purchase(
                CUSTOMER_1, THREE_SESSIONS.id(), Money.chf("450.00"), new SessionCount(5), NOW),
        ConsultationOfferingForPurchase.PACKAGE_CHANGED,
        CHANGED);
  }

  @Test
  void comparesThePriceAsMoney() {
    // "450" and "450.00" are the same money
    NewConsultationCase consultationCase =
        published()
            .purchase(CUSTOMER_1, THREE_SESSIONS.id(), Money.chf("450"), new SessionCount(3), NOW);

    assertThat(consultationCase.purchaseTerms().pricePaid()).isEqualTo(Money.chf("450.00"));
  }
}
