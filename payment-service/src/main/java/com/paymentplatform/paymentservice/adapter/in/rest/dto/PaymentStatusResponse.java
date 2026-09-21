package com.paymentplatform.paymentservice.adapter.in.rest.dto;

import com.paymentplatform.paymentservice.domain.model.Payment;
import com.paymentplatform.paymentservice.domain.model.PaymentStatus;

public record PaymentStatusResponse(String paymentId, PaymentStatus status) {

    public static PaymentStatusResponse from(Payment payment) {
        return new PaymentStatusResponse(payment.paymentId(), payment.status());
    }
}
