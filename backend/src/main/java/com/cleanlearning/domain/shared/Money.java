package com.cleanlearning.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * An amount in CHF, including VAT. It has exactly two decimals and is never negative, so {@code 450.0} and
 * {@code 450.00} are the same money.
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    public static final String CURRENCY = "CHF";

    public Money {
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Money must not be negative: " + amount);
        }
        try {
            amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Money has at most two decimals: " + amount, e);
        }
    }

    public static Money chf(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public String currency() {
        return CURRENCY;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }
}
