# Real-Time Payment & Transaction Processing Platform

A production-grade payment backend built to simulate a modern enterprise
banking/payment system. This is a long-term portfolio project focused on
**real backend engineering problems** — not a CRUD demo.

## Current status

**Milestone 1 — Foundation** (in progress)

A single runnable Spring Boot service (`payment-service`) with:

- Clean hexagonal (ports & adapters) package boundaries
- A `Payment` aggregate root with its initial lifecycle state (`CREATED`)
- REST API: create / get / get-status / list payments
- A consistent error model (`code`, `message`, `traceId`, `timestamp`, `details`)
- Trace-ID propagation into logs and error responses
- Actuator health checks
- Unit + web-slice tests

## Tech stack (growing)

Java 17 · Spring Boot 3.5 · Maven · PostgreSQL · MongoDB · Redis · Kafka ·
Docker · Kubernetes · OpenTelemetry · Prometheus · Grafana · GitHub Actions.

## Services

| Service | Responsibility | Status |
|---------|----------------|--------|
| payment-service | Payment lifecycle & orchestration | Milestone 1 |
| account-service | Account balances & transactions | planned |
| risk-service | Risk checks | planned |
| notification-service | Notifications | planned |
| audit-service | Audit trail (MongoDB) | planned |
| reconciliation-service | Detect inconsistencies | planned |
| api-gateway | Routing, auth, rate limiting | planned |

## Prerequisites

- JDK 17+ (project compiles with `--release 17`)
- Maven 3.9+ (or use the `./mvnw` wrapper)
- Docker + Docker Compose (for later milestones)

## Run (payment-service)

```bash
cd payment-service
./mvnw spring-boot:run
# or, once built:
./mvnw -q -DskipTests package && java -jar target/payment-service-0.0.1-SNAPSHOT.jar
```

Health check: http://localhost:8081/actuator/health

## Documentation

- [Architecture overview](docs/architecture/overview.md)
- [ADR-001 — Payment Service boundaries](docs/adr/ADR-001-payment-service-boundaries.md)
