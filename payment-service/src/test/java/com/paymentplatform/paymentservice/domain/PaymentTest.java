package com.paymentplatform.paymentservice.domain;

import com.paymentplatform.paymentservice.domain.model.Money;
import com.paymentplatform.paymentservice.domain.model.Payment;
import com.paymentplatform.paymentservice.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for the domain: no Spring, no database. If these pass, the
 * core business invariants hold regardless of which adapters are wired in.
 */
class PaymentTest {

    @Test
    void createsPaymentInCreatedState() {
        Payment payment = new Payment("ACC-1", "ACC-2", new Money(new BigDecimal("500.00"), "USD"));

        assertThat(payment.status()).isEqualTo(PaymentStatus.CREATED);
        assertThat(payment.paymentId()).startsWith("PAY-");
        assertThat(payment.money().amount()).isEqualByComparingTo("500.00");
        assertThat(payment.money().currency()).isEqualTo("USD");
    }

    @Test
    void rejectsSameSourceAndDestination() {
        assertThatThrownBy(() -> new Payment("ACC-1", "ACC-1", new Money(new BigDecimal("10"), "USD")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different");
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> new Money(new BigDecimal("0"), "USD"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalizesCurrencyToUpperCase() {
        Money money = new Money(new BigDecimal("10"), "usd");
        assertThat(money.currency()).isEqualTo("USD");
    }
}
