package com.cleanlearning.api.response;

import com.cleanlearning.domain.shared.value.Money;

/** The amount is a string with two decimals, for example {@code "450.00"}. */
public record MoneyResponse(String amount, String currency) {

  public static MoneyResponse from(Money money) {
    return new MoneyResponse(money.amount().toPlainString(), money.currency());
  }
}
