package com.paymentplatform.paymentservice.adapter.in.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * HTTP request body for POST /api/payments. Validation is declarative here so
 * malformed input is rejected before it reaches the application/domain layers.
 */
public record CreatePaymentRequest(
        @NotBlank(message = "sourceAccountId is required")
        String sourceAccountId,

        @NotBlank(message = "destinationAccountId is required")
        String destinationAccountId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        @Pattern(regexp = "[A-Za-z]{3}", message = "currency must be a 3-letter ISO code")
        String currency) {
}
