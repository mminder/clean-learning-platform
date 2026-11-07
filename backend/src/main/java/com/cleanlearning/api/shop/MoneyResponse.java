package com.cleanlearning.api.shop;

import com.cleanlearning.domain.shared.Money;

/**
 * Money as JSON: the amount is a string with two decimals, for example {@code "450.00"}.
 */
public record MoneyResponse(String amount, String currency) {

    static MoneyResponse from(Money money) {
        return new MoneyResponse(money.amount().toPlainString(), money.currency());
    }
}
