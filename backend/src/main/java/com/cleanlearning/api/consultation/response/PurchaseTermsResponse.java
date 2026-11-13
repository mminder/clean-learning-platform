package com.cleanlearning.api.consultation.response;

import com.cleanlearning.api.response.MoneyResponse;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;

/** A case's purchase terms, copied from the offering at the purchase (D-08). */
public record PurchaseTermsResponse(
    MoneyResponse pricePaid,
    int sessionCount,
    int sessionDurationMinutes,
    int cancellationDeadlineHours) {

  public static PurchaseTermsResponse from(PurchaseTerms terms) {
    return new PurchaseTermsResponse(
        MoneyResponse.from(terms.pricePaid()),
        terms.sessionCount().value(),
        terms.sessionDuration().minutes(),
        terms.cancellationDeadline().hours());
  }
}
