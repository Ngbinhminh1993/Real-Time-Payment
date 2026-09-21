# ADR-001 — Payment Service boundaries and internal structure

- **Status:** Accepted
- **Date:** 2026-09-21

## Context

We are starting the platform with the `payment-service`, which owns the
payment lifecycle. We need to decide (a) what this service is responsible for
and (b) how to structure its code so it can grow without becoming tangled.

## Decision

- `payment-service` owns the **payment lifecycle** (create → validate → risk →
  process → complete) and the `Payment` aggregate. It does **not** own account
  balances, risk rules, or notifications — those become separate services.
- Internally we use **hexagonal architecture**: `domain`, `application`, and
  `adapter/in|out` packages, with the dependency rule pointing inward.
- The persistence port (`PaymentRepository`) is an interface in the
  application layer; the first implementation is in-memory.

## Alternatives considered

- **Flat "MVC by layer" (controller/service/repository)** — faster to start,
  but business rules leak into infrastructure and it does not scale with
  complexity. Rejected.
- **Full DDD with many aggregates and value objects up front** — over-engineering
  at this stage. We introduce DDD concepts only as they earn their place.

## Consequences

- Slightly more files than a naive layout, but each has a clear home.
- Business rules are testable without Spring or a database.
- We can swap persistence / add Kafka without touching the domain.
