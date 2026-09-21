package com.paymentplatform.paymentservice.adapter.in.rest;

import com.paymentplatform.paymentservice.adapter.in.rest.dto.CreatePaymentRequest;
import com.paymentplatform.paymentservice.adapter.in.rest.dto.PaymentResponse;
import com.paymentplatform.paymentservice.adapter.in.rest.dto.PaymentStatusResponse;
import com.paymentplatform.paymentservice.application.CreatePaymentCommand;
import com.paymentplatform.paymentservice.application.PaymentService;
import com.paymentplatform.paymentservice.domain.model.Payment;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Driving adapter: exposes the payment use cases over REST.
 * This class only translates HTTP <-> application; it holds no business logic.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        CreatePaymentCommand command = new CreatePaymentCommand(
                request.sourceAccountId(),
                request.destinationAccountId(),
                request.amount(),
                request.currency());
        Payment payment = paymentService.createPayment(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(payment));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId) {
        return ResponseEntity.ok(PaymentResponse.from(paymentService.getPayment(paymentId)));
    }

    @GetMapping("/{paymentId}/status")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(@PathVariable String paymentId) {
        return ResponseEntity.ok(PaymentStatusResponse.from(paymentService.getPayment(paymentId)));
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getPayments() {
        List<PaymentResponse> payments = paymentService.getPayments().stream()
                .map(PaymentResponse::from)
                .toList();
        return ResponseEntity.ok(payments);
    }
}
