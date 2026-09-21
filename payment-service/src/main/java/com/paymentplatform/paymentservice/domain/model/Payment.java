package com.paymentplatform.paymentservice.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Payment aggregate root.
 *
 * It owns its own consistency rules (invariants): a payment is always created
 * in {@link PaymentStatus#CREATED}, source and destination must differ, and the
 * amount/currency are validated by {@link Money}. State changes will go through
 * methods on this class so invalid transitions can never be reached from outside.
 */
public final class Payment {

    private final String paymentId;
    private final String sourceAccountId;
    private final String destinationAccountId;
    private final Money money;
    private PaymentStatus status;
    private final Instant createdAt;

    public Payment(String sourceAccountId, String destinationAccountId, Money money) {
        Objects.requireNonNull(sourceAccountId, "sourceAccountId must not be null");
        Objects.requireNonNull(destinationAccountId, "destinationAccountId must not be null");
        Objects.requireNonNull(money, "money must not be null");

        if (sourceAccountId.equals(destinationAccountId)) {
            throw new IllegalArgumentException("source and destination accounts must be different");
        }

        // Simple unique ID for now. We will revisit ID strategy (ULID / DB
        // sequence / snowflake) when persistence and multi-instance scaling arrive.
        this.paymentId = "PAY-" + UUID.randomUUID();
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.money = money;
        this.status = PaymentStatus.CREATED;
        this.createdAt = Instant.now();
    }

    public String paymentId() {
        return paymentId;
    }

    public String sourceAccountId() {
        return sourceAccountId;
    }

    public String destinationAccountId() {
        return destinationAccountId;
    }

    public Money money() {
        return money;
    }

    public PaymentStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
