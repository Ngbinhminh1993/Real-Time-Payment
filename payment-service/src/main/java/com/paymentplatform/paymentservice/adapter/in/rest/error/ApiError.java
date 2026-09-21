package com.paymentplatform.paymentservice.adapter.in.rest.error;

import java.time.Instant;
import java.util.List;

/**
 * Consistent error body returned for every failed request.
 *
 * It intentionally exposes no internal detail (stack traces, SQL, bean names)
 * that could leak implementation information or aid an attacker.
 */
public record ApiError(
        String code,
        String message,
        String traceId,
        Instant timestamp,
        List<FieldError> details) {

    public record FieldError(String field, String message) {
    }

    public static ApiError of(ErrorCode code, String message, String traceId) {
        return new ApiError(code.name(), message, traceId, Instant.now(), List.of());
    }

    public static ApiError of(ErrorCode code, String message, String traceId, List<FieldError> details) {
        return new ApiError(code.name(), message, traceId, Instant.now(), details);
    }
}
