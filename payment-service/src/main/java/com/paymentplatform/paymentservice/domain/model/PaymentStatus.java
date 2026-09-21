package com.paymentplatform.paymentservice.domain.model;

/**
 * The lifecycle of a payment.
 *
 * The full state machine is defined here; transitions will be enforced as the
 * workflow grows. In Milestone 1 a payment is created in {@link #CREATED} only.
 */
public enum PaymentStatus {
    CREATED,
    VALIDATING,
    RISK_CHECKING,
    APPROVED,
    REJECTED,
    PROCESSING,
    DEBITING,
    CREDITING,
    COMPLETED,
    FAILED,
    CANCELLED
}
