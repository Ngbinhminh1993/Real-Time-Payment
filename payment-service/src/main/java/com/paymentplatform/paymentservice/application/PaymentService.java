package com.paymentplatform.paymentservice.application;

import com.paymentplatform.paymentservice.domain.exception.PaymentNotFoundException;
import com.paymentplatform.paymentservice.domain.model.Money;
import com.paymentplatform.paymentservice.domain.model.Payment;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service that orchestrates the payment use cases.
 *
 * It coordinates the domain model and the ports, but contains no HTTP, database,
 * or messaging details. Business rules live in the domain; this layer only wires
 * the flow together.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment createPayment(CreatePaymentCommand command) {
        Money money = new Money(command.amount(), command.currency());
        Payment payment = new Payment(command.sourceAccountId(), command.destinationAccountId(), money);
        return paymentRepository.save(payment);
    }

    public Payment getPayment(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    public List<Payment> getPayments() {
        return paymentRepository.findAll();
    }
}
