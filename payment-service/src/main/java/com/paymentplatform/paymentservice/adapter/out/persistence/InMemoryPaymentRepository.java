package com.paymentplatform.paymentservice.adapter.out.persistence;

import com.paymentplatform.paymentservice.application.PaymentRepository;
import com.paymentplatform.paymentservice.domain.model.Payment;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of the {@link PaymentRepository} port.
 *
 * Used only until PostgreSQL arrives in a later milestone; because the
 * application layer depends on the port (interface), swapping this adapter
 * requires no change to business logic.
 */
@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<String, Payment> store = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        store.put(payment.paymentId(), payment);
        return payment;
    }

    @Override
    public Optional<Payment> findById(String paymentId) {
        return Optional.ofNullable(store.get(paymentId));
    }

    @Override
    public List<Payment> findAll() {
        return List.copyOf(store.values());
    }
}
