package com.paymentplatform.paymentservice.application;

import java.math.BigDecimal;

/**
 * Application-layer input for the "create payment" use case.
 *
 * The web adapter maps its HTTP DTO to this command, keeping HTTP concerns
 * (annotations, JSON) out of the application layer.
 */
public record CreatePaymentCommand(
        String sourceAccountId,
        String destinationAccountId,
        BigDecimal amount,
        String currency) {
}
