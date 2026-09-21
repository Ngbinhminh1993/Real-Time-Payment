# Architecture & Planning Guide (plain English)

> This guide explains **what we're building and in what order, and why** — for
> someone unfamiliar with Java Spring Boot or backend architecture. No framework
> jargon required. It complements
> [beginner-guide-flow-and-structure.md](beginner-guide-flow-and-structure.md),
> which explains the *code*; this one explains the *system* and the *plan*.

## 1. What we're building (the 30,000-foot view)

A **payment platform** — the kind of backend that powers a money-transfer app
(think Venmo, Stripe, or your bank's "send money" feature). The core job is
simple to say and hard to do well:

> **Move money from account A to account B — reliably, traceably, and safely.**

Why it's genuinely hard (and why this project is interesting):

- **Money must be exact.** No rounding, no "close enough," no double-charging.
- **Failures are guaranteed.** A network blip, a retry, a crash — the system
  must never lose a payment or process it twice.
- **Concurrency.** Many payments happen at once; they must not corrupt each other.
- **Audit & trust.** Regulators and customers must be able to see *exactly* what
  happened, later.
- **Fraud & risk.** Some payments should be blocked before they happen.

Everything in the architecture exists to serve one of those bullet points.

### A mental model

Instead of one giant program that does everything, we build the system as a set
of **small, focused programs ("services") that talk to each other over the
network** — a *distributed system*. Each service is like a specialist in a
company: accounting doesn't do marketing, and neither does the other's job.

## 2. The cast of characters (the services)

Here's the full roster. The first one is already built; the rest are planned.

| Service | What it does (plain words) | Status |
|---------|----------------------------|--------|
| **payment-service** | The hero. Owns the *life of a payment*: create it, validate it, process it, mark it done/failed. | ✅ Milestone 1 built |
| **account-service** | Keeps account balances and records debits/credits. "Does Alice have enough money?" | planned |
| **risk-service** | Decides if a payment looks fraudulent and should be blocked. | planned |
| **notification-service** | Sends "your payment was received / completed" messages (email, push, SMS). | planned |
| **audit-service** | An **append-only** log of everything that happened, for compliance. | planned |
| **reconciliation-service** | The watchdog that periodically checks "do all the books add up?" and flags mismatches. | planned |
| **api-gateway** | The single front door for outside clients: routing, authentication, rate limiting. | planned |

**Why split into services at all?** Three reasons:
1. **Independent scaling** — if notifications are slow, you scale that one, not everything.
2. **Independent teams/ownership** — each service can be built and deployed separately.
3. **Failure isolation** — if notifications crash, payments keep working.

The honest caveat (important!): splitting into many services has a real cost —
more moving parts, more networking, more operations. That's why we **grow into
it incrementally** rather than build all seven on day one. (More in §7.)

## 3. How the pieces talk to each other (communication)

Services communicate in two fundamentally different styles:

| Style | Real-world analogy | When to use it |
|-------|--------------------|----------------|
| **Synchronous (REST)** | A phone call — you ask, wait for the answer. | When the caller needs an answer *now*, e.g. "create a payment" returns the new payment ID. |
| **Asynchronous (events / Kafka)** | A notice on a bulletin board — you post it and walk away; whoever cares reads it later. | When others should *react* to something but shouldn't block it. |

The golden rule for a payments system:

> **A payment should not be delayed by "the email was sent."**

Concretely: when a payment completes, `payment-service` *announces* "payment
COMPLETED" on a **message bus (Kafka)** and moves on. `notification-service` and
`audit-service` hear that announcement and do their work independently. If the
email server is down, the payment still completes; the notification just retries
later. This decoupling is the heart of a resilient payments platform.

**Why the name "Kafka"?** It's just the popular open-source message bus we'll
use — think of it as a very fast, durable bulletin board that keeps every posted
message safe until everyone who needs it has read it.

**When *do* we use synchronous calls?** For the things a client truly needs an
answer to right away — like "create this payment now" or "what's the status of
payment X?" That's the REST API you already saw in `payment-service`.

## 4. Where data lives (database-per-service)

The rule is: **each service owns its own database, and no service ever reads
another service's database directly.**

- If `account-service` needs to know about a payment, it *listens for the event*
  or *calls an API* — it never reaches into `payment-service`'s database.
- **Why?** Direct database sharing is a hidden trap: two services become glued
  together through shared tables, so you can't change one without breaking the
  other, and you can't scale them independently. Owning your own database keeps
  services genuinely independent.

Not every service needs the same *kind* of database — we pick the right tool per
job (a "polystore"):

| Technology | What it's for | Which service |
|------------|---------------|---------------|
| **PostgreSQL** | Relational data with strict consistency (accounts, payments, balances). | payment, account, risk |
| **MongoDB** | Fast append-only records (the audit trail). | audit |
| **Redis** | Super-fast in-memory storage for caching and rate limiting. | api-gateway |
| **Kafka** | The durable message bus (the bulletin board from §3). | all services |

> **Key idea:** money-critical state lives in a relational database (PostgreSQL)
> with strict rules; high-volume "write once, read many" data lives in MongoDB;
> speed-critical temporary data lives in Redis; everything communicates through
> Kafka. One tool per job.

## 5. How each service is built inside (one recipe, used everywhere)

Even though the seven services do different jobs, they all share the **same
internal recipe**, so learning one teaches you all of them:

```
   Outside (HTTP requests, events)
            │
   ┌────────▼─────────┐   adapters: translate outside <-> inside
   │   application    │   orchestration: the "manager" steps
   │      domain      │   the RULES (pure business logic, no plumbing)
   └────────▲─────────┘
            │
   Outside (databases, message bus)
```

The one rule that matters: **the rules sit in the middle and know nothing about
HTTP, databases, or Kafka. The plumbing sits on the edges and points inward.**

Why this matters, concretely (already true in `payment-service`):
- The `Payment`/`Money` rules are testable with plain Java — no server, no database.
- Next milestone swaps the in-memory storage for PostgreSQL by plugging in a new
  "edge" piece, and the rules don't change.

This pattern is called **hexagonal architecture** ("ports & adapters") — but
don't worry about the name; the idea is just "rules in the middle, plumbing on
the edges." For the full explanation, see
[beginner-guide-flow-and-structure.md](beginner-guide-flow-and-structure.md) §7
and `../docs/architecture/overview.md`.

## 6. The "glue" every service needs (cross-cutting concerns)

Some concerns aren't about any one service — they cut across all of them. We
build these once and reuse the pattern everywhere:

| Concern | Plain meaning | Status |
|---------|---------------|--------|
| **Trace ID** | Tag every request with a unique ID so you can find all its logs/errors together, across services. | ✅ done in payment-service |
| **Consistent errors** | Every failure returns the same shape, so clients always know what to expect. | ✅ done |
| **Idempotency** | "Doing the same request twice must not double-charge." The #1 payments concern; handles retries. | ⏳ upcoming (M4) |
| **Observability** | Logs, metrics, and tracing so you can *see* what a distributed system is doing. | ⏳ upcoming (M6) |
| **Security** | Authentication ("who are you"), authorization ("are you allowed"), secret management. | ⏳ upcoming |

The first two already exist in `payment-service`; the rest are deliberately
scheduled later in the roadmap — build the skeleton, then add the armor.

## 7. The roadmap — what we build, in what order, and why

The plan follows one principle: **small, working, tested steps** ("thin vertical
slices"). We never build a big pile of code and then hope it works; each
milestone leaves the system *runnable*.

| # | Milestone | What & why (plain words) | Status |
|---|-----------|--------------------------|--------|
| **M1** | Foundation | One service, clean skeleton, in-memory storage. Proves the recipe works before we add weight. | ✅ done |
| **M2** | Persistence (PostgreSQL + Flyway) | Make payments **survive a restart**. Introduces the "each service owns a database" pattern for real. | ▶ next |
| **M3** | Payment state machine | The actual *life* of a payment: `CREATED → … → COMPLETED/FAILED`, with illegal jumps blocked. The real business logic. | planned |
| **M4** | Money correctness + idempotency + locking | The hard parts of payments: retries must not double-charge, and concurrent updates must not corrupt state. | planned |
| **M5** | Events (Kafka) + account-service | First cross-service flow — a completed payment announces itself, and another service reacts. Proves the decoupling from §3. | planned |
| **M6** | Observability | Logs/metrics/tracing so you can *see* a multi-service system working (and debug it). | planned |
| **M7** | Docker + CI/CD | Package and deploy reproducibly; automate build/test on every change. | planned |
| **M8+** | Remaining services + gateway | risk, notification, audit, reconciliation, and the api-gateway round out the platform. | planned |

**Why this order?**

- **M1 before everything** — you can't add armor (M4/M6) until a working skeleton exists.
- **M2 before M3** — the state machine needs somewhere durable to keep state.
- **M3 before M4** — you can't protect *transitions* (locking, idempotency) until transitions exist.
- **M5 before M6** — observability is only *needed* once you have multiple services and async events to debug.
- **M7 before M8** — get one service fully deployed/tested, then clone the pattern for the rest.

This ordering is deliberately **risk-first**: we do the scary money stuff (M3/M4)
early, before the system grows large enough that change becomes expensive.

## 8. How decisions get made & recorded

Big decisions are written down as **ADRs (Architecture Decision Records)** — a
short "what did we decide, why, and what did we reject" note, so future-you (and
reviewers) can see the reasoning.

- Already recorded: `../docs/adr/ADR-001-payment-service-boundaries.md`.
- Each significant choice in this guide (§2–§6) earns its own ADR as we reach it.

We also follow a mentoring rhythm (see [README.md](README.md)): **you implement,
I guide and review** — one milestone at a time, each one run and verified before
moving on.

## 9. Glossary (plain)

- **Service** — a self-contained program with one job, reachable over the network.
- **Microservices / distributed system** — a system built from many small services.
- **API** — a service's public "menu" of things you can ask it to do (over HTTP).
- **REST** — the common style of building HTTP APIs (URLs + JSON).
- **Event** — an announcement that "something happened" (e.g. "payment completed").
- **Message bus (Kafka)** — the durable bulletin board that delivers events.
- **Database** — durable storage that survives restarts.
- **Schema migration (Flyway)** — version-controlled changes to a database's structure.
- **Hexagonal / ports & adapters** — the "rules in the middle, plumbing on the edges" recipe (§5).
- **Idempotency** — doing the same operation twice has the same effect as once.
- **Observability** — the ability to see a system's internal state from its logs/metrics/traces.
- **Scalability** — the ability to handle more load by adding more resources.

## 10. Things still open for us to decide together

1. **Growth path** — the destination is a microservices platform (this guide), but
   we sequence it incrementally. If we ever want to *keep* it as fewer, larger
   services, that's a valid choice to revisit.
2. **Which service to build after account-service** in M8 — risk, notification,
   audit, or reconciliation? We'll pick based on what best shows the next skill.
3. **Deployment target** (M7) — plain Docker Compose first, or straight to a
   Kubernetes cluster? (Compose is simpler to start; K8s is more impressive for a
   portfolio.)

These are recorded here so we decide them *explicitly* rather than by accident.

