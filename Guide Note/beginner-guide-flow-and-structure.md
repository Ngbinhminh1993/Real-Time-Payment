# Beginner's Guide — How the Payment Service Flows & Is Structured

> This guide assumes you can read basic Java but are **new to Spring Boot and
> backend architecture**. It explains the *real* code in `../payment-service/`
> in plain language, with an analogy first so the rest clicks.

## 1. The mental model (read this first)

Think of the payment service as a **small business** with a few clear roles:

| Role | The code | Job |
|------|----------|-----|
| **Front desk** | `PaymentController` | Talks to the outside world (HTTP/JSON). Fills in forms. Makes **no** business decisions. |
| **Manager** | `PaymentService` | Orchestrates: "check the rules, file it, tell the front desk the result." |
| **Rulebook** | `Payment`, `Money`, `PaymentStatus` | The actual rules of what a *valid* payment is. No HTTP, no database, pure rules. |
| **Filing cabinet** | `PaymentRepository` + `InMemoryPaymentRepository` | Stores payments. The manager only talks to a **label on the drawer** (an interface), not the drawer itself — so you can swap the drawer without retraining the manager. |

That last point is the whole trick of the architecture: **the manager depends on a
label ("I need save/find/list"), not on a specific drawer.** Today the drawer is
an in-memory map; next milestone it becomes PostgreSQL — and the manager's
instructions don't change at all.

## 2. The big picture

```
 Browser / curl / Postman
        │  HTTP + JSON
        ▼
 ┌─────────────────────────────┐
 │  TraceIdFilter              │  stamps a request ID on everything
 ├─────────────────────────────┤
 │  PaymentController (front)  │  turns JSON <-> objects, validates input
 ├─────────────────────────────┤
 │  PaymentService (manager)   │  orchestrates the use case
 ├─────────────────────────────┤
 │  Payment / Money (rulebook) │  enforces invariants
 ├─────────────────────────────┤
 │  PaymentRepository (label)  │  interface: save / findById / findAll
 │  InMemoryPaymentRepository  │  the actual drawer (a Map) behind the label
 └─────────────────────────────┘
```

## 3. A request's journey (the most important section)

Let's follow one real request:

```
curl -X POST http://localhost:8081/api/payments \
  -H 'Content-Type: application/json' \
  -d '{"sourceAccountId":"ACC-1","destinationAccountId":"ACC-2","amount":500,"currency":"USD"}'
```

1. **`TraceIdFilter` runs first.** It looks for an `X-Trace-Id` header; if none,
   it makes a random UUID. It stores that ID so every log line and error response
   from this request carries the same ID. (This is how you later find "the one
   request that failed" in a sea of logs.)

2. **Spring routes the request to `PaymentController.createPayment`.** The
   `@PostMapping` says "handle POST to `/api/payments`." Spring automatically
   parses the JSON body into a `CreatePaymentRequest` object (`@RequestBody`) and
   checks its rules (`@Valid`) — e.g. `@DecimalMin("0.01")` rejects `amount: 0`
   **before** it ever reaches your business code.

3. **The controller converts the DTO into a `CreatePaymentCommand`.** It does
   this because a `CreatePaymentRequest` carries HTTP-specific annotations;
   the business layer shouldn't know or care about HTTP. The command is just
   four plain values.

4. **`PaymentService.createPayment` builds the domain objects:**
   - `new Money(amount, currency)` — the `Money` record's constructor *rejects*
     invalid money (amount ≤ 0, currency not 3 letters). An invalid `Money`
     simply cannot be created.
   - `new Payment(source, destination, money)` — checks source ≠ destination,
     generates a `PAY-…` ID, sets status to `CREATED`, stamps `createdAt`.

5. **`paymentRepository.save(payment)`** stores it in the in-memory `Map`.

6. **The controller turns the `Payment` back into a `PaymentResponse`** (JSON
   shape) and returns HTTP `201 Created` with the body.

7. **If anything threw an exception anywhere along the way**, `GlobalExceptionHandler`
   catches it and returns one consistent error shape:
   `{code, message, traceId, timestamp, details}` — so clients always know what
   to expect, good request or bad.

That's the whole flow. Everything else in the codebase is supporting that loop.

## 4. The folders & files (what each one does)

All paths below are under `payment-service/src/main/java/com/paymentplatform/paymentservice/`.

### `domain/` — the rulebook (knows *nothing* about HTTP or databases)

| File | What it is, in plain words |
|------|---------------------------|
| `domain/model/Payment.java` | The payment itself. A `final` class with a constructor that enforces its rules. Note the **accessor methods** are named `paymentId()`, `status()`, etc. (not `getPaymentId()`). |
| `domain/model/Money.java` | A `record` = an immutable "amount + currency" pair. Its compact constructor enforces "positive amount, 3-letter currency." |
| `domain/model/PaymentStatus.java` | An `enum`: the fixed list of life stages (`CREATED`, `VALIDATING`, … `COMPLETED`, `FAILED`). Only `CREATED` is used so far. |
| `domain/exception/PaymentNotFoundException.java` | A custom exception for "that ID doesn't exist." |

### `application/` — the manager (orchestrates, but no HTTP/SQL)

| File | What it is |
|------|-----------|
| `application/PaymentService.java` | The use-case orchestrator: create / get / list. `@Service` marks it as a Spring-managed object. |
| `application/PaymentRepository.java` | The **port** (an `interface`): the *label on the drawer*. Defines `save`, `findById`, `findAll`. |
| `application/CreatePaymentCommand.java` | A plain `record` carrying the four inputs, free of HTTP annotations. |

### `adapter/in/rest/` — the front desk (translates HTTP ↔ objects)

| File | What it is |
|------|-----------|
| `adapter/in/rest/PaymentController.java` | The `@RestController`. Maps URLs (`@PostMapping`, `@GetMapping`) to methods. No business logic. |
| `adapter/in/rest/TraceIdFilter.java` | A servlet filter that runs before every request (see §3, step 1). |
| `adapter/in/rest/dto/CreatePaymentRequest.java` | The **request DTO** — the JSON body shape, with validation annotations. |
| `adapter/in/rest/dto/PaymentResponse.java` | The **response DTO** — what we send back. Has a `from(Payment)` factory to convert a domain object. |
| `adapter/in/rest/dto/PaymentStatusResponse.java` | A smaller response for the `/status` endpoint. |
| `adapter/in/rest/error/ApiError.java` | The one error shape every failure returns. |
| `adapter/in/rest/error/ErrorCode.java` | The stable, machine-readable error codes. |
| `adapter/in/rest/error/GlobalExceptionHandler.java` | `@RestControllerAdvice` that converts exceptions → `ApiError` + the right HTTP status. |

### `adapter/out/persistence/` — the filing cabinet (behind the label)

| File | What it is |
|------|-----------|
| `adapter/out/persistence/InMemoryPaymentRepository.java` | Implements `PaymentRepository` using a `ConcurrentHashMap`. `@Repository` marks it as a Spring bean. This is what gets **replaced by PostgreSQL** in the next milestone. |

### Root & resources

| File | What it is |
|------|-----------|
| `PaymentServiceApplication.java` | The `main` method + `@SpringBootApplication` — this is what *starts* the whole app. |
| `resources/application.yml` | Configuration: app name, port `8081`, actuator endpoints, logging level. |
| `resources/logback-spring.xml` | Log format config — includes the `%X{traceId}` so trace IDs appear in log lines. |
| `pom.xml` | Maven's "shopping list" of dependencies + build settings. |

## 5. Spring Boot in plain English

Spring Boot is a framework that **takes care of the plumbing** so you can focus
on the rules. Three ideas explain 90% of what you see:

1. **Annotations are labels.** `@RestController`, `@Service`, `@Repository`,
   `@Component` all tell Spring: *"please create and manage an instance of this
   class for me."* A class with one of these labels is a **bean**.

2. **Dependency injection (DI).** You almost never write `new PaymentService(...)`.
   Instead you declare what you need in the constructor, and Spring hands it to
   you. Example — `PaymentController`'s constructor takes a `PaymentService`:
   ```java
   public PaymentController(PaymentService paymentService) {
       this.paymentService = paymentService;
   }
   ```
   Spring sees this, finds/makes the `PaymentService` bean, and passes it in.
   *Why it matters:* the controller never decides *which* repository or service
   to use — Spring wires it, so swapping pieces is easy and testing is easy.

3. **Spring maps URLs to methods.** `@GetMapping("/{paymentId}")` means "when a
   GET arrives at `/api/payments/PAY-123`, call this method and put `PAY-123`
   into the `paymentId` parameter." `@RequestBody` means "parse the JSON into
   this object." `@PathVariable` reads a piece of the URL.

**Maven (`pom.xml`) in one sentence:** it's the list of libraries your project
uses (each `<dependency>`) plus how to build it. `./mvnw` downloads Maven itself
and runs it, so everyone builds with the same version.

## 6. Java features used (in plain English)

You'll see these everywhere; here's what they mean:

| Feature | Plain meaning | Where you'll see it |
|---------|---------------|---------------------|
| `record` | A compact, immutable data holder. Auto-generates a constructor, accessors, `equals`, `hashCode`, `toString`. | `Money`, `CreatePaymentCommand`, all DTOs, `ApiError` |
| `interface` | A **contract**: "anything claiming to be a `PaymentRepository` must provide `save`, `findById`, `findAll`." | `PaymentRepository` |
| `implements` | "This class fulfills that contract." | `InMemoryPaymentRepository implements PaymentRepository` |
| `enum` | A fixed set of named constants. | `PaymentStatus`, `ErrorCode` |
| `Optional<T>` | A box that may hold a value or be empty — the safe alternative to `null`. | `PaymentRepository.findById` |
| `BigDecimal` | Exact decimal numbers for money. **Never** use `double`/`float` for money (they round badly). | `Money.amount`, `CreatePaymentCommand.amount` |
| `final` | "Set once, never change after." Makes objects safe to share. | fields in `Payment`, the `Payment` class itself |
| `static` factory | A static helper method that builds/convert an object, e.g. `PaymentResponse.from(payment)`. | `PaymentResponse.from`, `Money.of`, `ApiError.of` |
| compact constructor | A `record` constructor written as `public Money { ... }` — runs validation on every construction. | `Money` |

## 7. The architecture "why" (hexagonal in one paragraph)

The rule is simple: **the business rules (`domain`) must not depend on
infrastructure (HTTP, database).** Infrastructure depends on the rules instead.

Why you should care, concretely:
- **Testing** — you can test `Payment` and `PaymentService` with no Spring, no
  database, no HTTP. Pure Java.
- **Swapping** — next milestone replaces the in-memory `Map` with PostgreSQL by
  adding a *new* class behind the same `PaymentRepository` interface. `Payment`
  and `PaymentService` don't change. That's the payoff.
- **Clarity** — when a bug appears, you know *where* to look: is it a rule problem
  (`domain`), a wiring problem (`application`), or an I/O problem (`adapter`)?

For the fuller version, see `../docs/architecture/overview.md`.

## 8. Glossary / cheat sheet

- **DTO** — Data Transfer Object; a simple object that crosses a boundary (HTTP
  request/response). It exists so JSON shapes and domain objects can differ.
- **Port** — an interface the application depends on (`PaymentRepository`).
- **Adapter** — a concrete implementation plugged into a port (the in-memory repo).
- **Bean** — an object Spring creates and manages for you.
- **DI (dependency injection)** — Spring passing the beans a class needs into its
  constructor, instead of the class making them itself.
- **Invariant** — a rule that must always be true (amount > 0, source ≠ destination).
- **Value object vs entity** — an entity has identity (`Payment`, even if fields
  match it's a different payment); a value object is equal by value (`Money`).
- **Trace ID** — a unique ID attached to one request so its logs and errors can be
  found together.

## 9. How to explore it yourself

1. Open `PaymentController.java` and trace `createPayment` line by line, naming
   which file each call reaches.
2. In `PaymentService.createPayment`, add a temporary `System.out.println` to
   print the generated `paymentId`, run the app, POST a payment, and watch it
   appear in the console.
3. Change `Payment`'s constructor so source==destination is *allowed*, run the
   domain test, and watch it fail — then revert. (Understanding *why it fails*
   is understanding the architecture.)
4. Break the validation: remove `@DecimalMin` from `CreatePaymentRequest`, POST
   `amount: 0`, and see *where* the failure now happens (hint: `Money` still
   catches it — two layers of defense).


