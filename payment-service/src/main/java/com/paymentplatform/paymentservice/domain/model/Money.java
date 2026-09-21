package com.paymentplatform.paymentservice.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing an amount of money in a currency.
 *
 * A "value object" has no identity of its own: two Money objects are the same
 * if their amount and currency are equal. It is immutable, and its invariants
 * are enforced in the (compact) constructor so an invalid Money can never exist.
 */
public record Money(BigDecimal amount, String currency) {

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        currency = currency.trim().toUpperCase();
        if (currency.length() != 3) {
            throw new IllegalArgumentException("currency must be a 3-letter ISO code");
        }
    }

    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }
}
