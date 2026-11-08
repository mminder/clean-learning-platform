package com.cleanlearning.api.response;

import com.cleanlearning.domain.shared.value.Money;

/**
 * Money as JSON: the amount is a string with two decimals, for example {@code "450.00"}. Shared by every feature's
 * responses, so there is one {@code MoneyResponse} schema in the OpenAPI document.
 */
public record MoneyResponse(String amount, String currency) {

    public static MoneyResponse from(Money money) {
        return new MoneyResponse(money.amount().toPlainString(), money.currency());
    }
}
