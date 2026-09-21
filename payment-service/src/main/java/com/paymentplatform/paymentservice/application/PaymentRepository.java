package com.paymentplatform.paymentservice.application;

import com.paymentplatform.paymentservice.domain.model.Payment;

import java.util.List;
import java.util.Optional;

/**
 * Port (out): the persistence contract the application layer depends on.
 *
 * The concrete storage (in-memory now, PostgreSQL later) lives in the adapter
 * layer, so domain/application never depend on infrastructure. This is the
 * dependency-inversion principle in action.
 */
public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(String paymentId);

    List<Payment> findAll();
}
