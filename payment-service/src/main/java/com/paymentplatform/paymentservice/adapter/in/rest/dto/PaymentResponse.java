package com.paymentplatform.paymentservice.adapter.in.rest.dto;

import com.paymentplatform.paymentservice.domain.model.Payment;
import com.paymentplatform.paymentservice.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        String paymentId,
        String sourceAccountId,
        String destinationAccountId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        Instant createdAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.paymentId(),
                payment.sourceAccountId(),
                payment.destinationAccountId(),
                payment.money().amount(),
                payment.money().currency(),
                payment.status(),
                payment.createdAt());
    }
}
