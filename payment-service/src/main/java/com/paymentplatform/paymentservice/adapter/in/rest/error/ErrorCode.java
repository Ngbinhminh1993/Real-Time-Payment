package com.paymentplatform.paymentservice.adapter.in.rest.error;

/**
 * Stable, machine-readable error codes used in the consistent error model.
 * These become part of the public API contract, so avoid renaming them freely.
 */
public enum ErrorCode {
    VALIDATION_ERROR,
    PAYMENT_NOT_FOUND,
    INVALID_REQUEST,
    INTERNAL_ERROR
}
