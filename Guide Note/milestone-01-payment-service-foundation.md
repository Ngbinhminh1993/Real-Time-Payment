# Milestone 01 — Payment Service Foundation (review + exercises)

> This milestone is **already implemented** in `../payment-service/` as your
> reference anchor. Your job now is to *understand* it deeply and *prove* that
> understanding with the exercises at the end. Do not skip the exercises —
> reading is not the same as knowing.

## 1. What exists and why

The service is split by the **hexagonal (ports & adapters)** dependency rule:

```
        adapter/in/rest   (HTTP controllers, DTOs, error model, trace filter)
                |
                v
        application        (PaymentService use case, PaymentRepository port, command)
                |
                v
          domain           (Payment aggregate, Money value object, PaymentStatus)
                ^
                |
        adapter/out/…      (InMemoryPaymentRepository — will become PostgreSQL)
```

**The one rule that matters:** *dependencies point inward.*
`domain` depends on nothing. `application` depends on `domain`.
`adapters` depend on `application`/`domain`. Nothing inward depends outward.

## 2. The flow of code — trace a request end-to-end

Reading the code gets much easier if you separate **two phases**: what happens
*once* when the app starts, and what happens on *every* request. All paths below
are relative to `payment-service/src/main/java/com/paymentplatform/paymentservice/`.

### Phase A — startup (happens once)

1. `PaymentServiceApplication.main(...)` calls `SpringApplication.run(...)`.
2. Spring **component-scans** the package: every class annotated with
   `@RestController`, `@Service`, `@Repository`, `@Component`, or
   `@RestControllerAdvice` gets **one** instance created (a *bean*).
3. Spring **wires dependencies** via constructor injection. It sees that
   `PaymentController` needs a `PaymentService`; that `PaymentService` needs a
   `PaymentRepository`; and that `InMemoryPaymentRepository` is the only bean
   implementing `PaymentRepository`. So it builds the whole chain for you.
4. The embedded web server (Tomcat) starts listening on port `8081`
   (from `application.yml`).

That's why you never write `new PaymentService(...)` yourself — Spring owns
object creation and wiring.

### Phase B — `POST /api/payments` (the happy path)

```
HTTP request
   → TraceIdFilter            (stamp a trace ID on the request)
   → DispatcherServlet        (Spring's router: match URL + method)
   → PaymentController.createPayment
   → PaymentService.createPayment
   → Money → Payment → PaymentRepository.save
   → PaymentResponse.from     → 201 Created + JSON
```

Step by step:

1. **`TraceIdFilter.doFilterInternal`** runs before any controller. It reads the
   `X-Trace-Id` header (or generates a UUID), stores it in the **MDC** (a
   per-thread map that logging reads), echoes it as a response header, and
   clears it in a `finally` block.
2. **`DispatcherServlet`** (Spring MVC's router) matches `POST /api/payments` to
   `PaymentController.createPayment`, because of `@RequestMapping("/api/payments")`
   + `@PostMapping`.
3. **Jackson** (the JSON library) parses the body into a `CreatePaymentRequest`.
   Because the parameter is `@Valid`, **Bean Validation** runs the annotations
   (`@NotBlank`, `@NotNull`, `@DecimalMin`, `@Pattern`). If any fail, Spring
   throws `MethodArgumentNotValidException` *before* your code runs (see the
   error paths below).
4. **The controller maps to a command** — it copies the four fields into a
   `CreatePaymentCommand`, so the `application` layer never sees the HTTP-only
   request object.
5. **`PaymentService.createPayment`**:
   - `new Money(amount, currency)` → the compact constructor validates
     `amount > 0` and a 3-letter currency (normalizing case).
   - `new Payment(source, destination, money)` → rejects `source == destination`,
     generates `PAY-<uuid>`, sets `status = CREATED`, stamps `createdAt`.
   - `paymentRepository.save(payment)` → `InMemoryPaymentRepository` does
     `store.put(payment.paymentId(), payment)` (a `ConcurrentHashMap`).
6. **`PaymentResponse.from(payment)`** converts the domain object to the JSON
   shape; the controller returns `ResponseEntity.status(CREATED).body(...)`.
   Jackson serializes it; Tomcat sends `201 Created`.

### Phase B — the error paths

Exceptions funnel into one place, `GlobalExceptionHandler` (`@RestControllerAdvice`),
which maps each to the consistent `ApiError` shape:

| What goes wrong | Exception thrown | Handler | HTTP |
|-----------------|------------------|---------|------|
| Validation fails (step 3) | `MethodArgumentNotValidException` | `handleValidation` | 400 |
| Unknown payment ID | `PaymentNotFoundException` (from `PaymentService.getPayment`) | `handleNotFound` | 404 |
| Bad domain rule reaches domain (e.g. `amount ≤ 0`) | `IllegalArgumentException` | `handleIllegalArgument` | 400 |
| Anything else | `Exception` | `handleUnexpected` | 500 |

Notice the **two layers of defense**: bad input is often caught twice — once by
`@Valid` at the HTTP edge (friendly per-field messages), and again by the domain
constructors (guaranteeing an invalid object can never exist at all).

### Phase B — the `GET` flows (shorter)

- `GET /api/payments/{paymentId}` → `getPayment` → `PaymentService.getPayment` →
  `findById(id)` returns an `Optional` → if empty, throws
  `PaymentNotFoundException`; else `PaymentResponse.from(...)` → 200.
- `GET /api/payments/{paymentId}/status` → same lookup, returns the smaller
  `PaymentStatusResponse` (just `paymentId` + `status`).
- `GET /api/payments` → `getPayments` → `findAll()` → map each to a
  `PaymentResponse` → 200 list.

### Where each concern lives (quick map)

| Concern | Class |
|---------|-------|
| HTTP ↔ object translation | `PaymentController`, `dto/*` |
| HTTP-level input validation | annotations on `CreatePaymentRequest` |
| Business rules | `Payment`, `Money` |
| Use-case orchestration | `PaymentService` |
| Persistence | `PaymentRepository` (port) + `InMemoryPaymentRepository` (adapter) |
| Trace ID | `TraceIdFilter` |
| Error mapping | `GlobalExceptionHandler` + `ApiError` + `ErrorCode` |

## 3. Concepts to understand (What / Why / How)

### 3.1 Entity vs Value Object

- **Entity** — has an *identity* that survives changes. Two payments are
  different even if all fields are equal, because each has a unique `paymentId`.
  → `Payment`.
- **Value Object** — has *no* identity; equality is by value. Two
  `Money("10","USD")` are the same thing. Immutable. → `Money`.

Open `domain/model/Money.java`. Notice the **compact constructor**: the
invariants (amount > 0, 3-letter currency) are enforced *at creation*, so an
invalid `Money` can never exist anywhere in the system.

### 3.2 Aggregate root

`Payment` is the **aggregate root** — the single entry point through which its
internal rules are enforced. Source ≠ destination is checked in its constructor.
Later, *all* state changes will go through methods on `Payment`, so invalid
transitions can never be triggered from outside.

### 3.3 Port & adapter (dependency inversion)

- `application/PaymentRepository.java` is a **port (out)** — an interface the
  application depends on.
- `adapter/out/persistence/InMemoryPaymentRepository.java` **implements** it.

Because `PaymentService` only knows the interface, swapping in-memory → PostgreSQL
later requires **zero changes** to the domain and application layers. That is the
whole point.

### 3.4 Application service

`application/PaymentService.java` orchestrates the use case but holds **no HTTP,
no SQL, no JSON**. It turns a `CreatePaymentCommand` into a `Payment` and saves it.

### 3.5 Consistent error model + trace ID

- `adapter/in/rest/error/ApiError.java` — every failure returns
  `{code, message, traceId, timestamp, details}`. No stack traces or SQL leak out.
- `adapter/in/rest/TraceIdFilter.java` — reads `X-Trace-Id` (or generates one),
  puts it in the **MDC**, echoes it as a header, and clears it in `finally`.
- `resources/logback-spring.xml` — the log pattern includes `%X{traceId:-}`,
  so the same trace ID links a request to its log lines and error responses.

## 4. Run and verify it yourself

```bash
cd payment-service
./mvnw test                       # 8 tests should pass
./mvnw spring-boot:run            # then, in another terminal:

curl http://localhost:8081/actuator/health
curl -X POST http://localhost:8081/api/payments \
  -H 'Content-Type: application/json' \
  -d '{"sourceAccountId":"ACC-1","destinationAccountId":"ACC-2","amount":500,"currency":"USD"}'
```

Also try an invalid request (amount `0`, currency `US`) and a `GET /api/payments/PAY-404`
to see the error model + trace IDs in action.

## 5. Exercises (do these yourself — this is where the learning happens)

1. **Trace a request end-to-end** by hand. Draw (on paper) what happens when
   `POST /api/payments` arrives: which class is hit first, second, third. Name
   each layer crossed.
2. **Add a new invariant** in the domain: reject an amount greater than
   `1,000,000` in `Money`. Write a unit test for it first (TDD), watch it fail,
   then make it pass.
3. **Add a filter to the list endpoint**: `GET /api/payments?currency=USD`
   should return only USD payments. Decide *where* the filtering belongs
   (controller? service? repository?) and justify it in one sentence.
4. **Explain the `@MockitoBean`** in `PaymentControllerTest` — what is being
   mocked, and why can that test run without a database?
5. **Write a new web-slice test** for the `GET /{paymentId}/status` endpoint.
6. **Break it on purpose**: in `TraceIdFilter`, remove the `finally` MDC cleanup.
   What observable problem would that cause in production? (Think: thread pools.)

## 6. Interview questions (answer in your own words)

1. Why is `PaymentRepository` in `application`, but `InMemoryPaymentRepository`
   in `adapter/out`? What would break if you put them in the same package?
2. What is the difference between an entity and a value object? Which is
   `Payment`, which is `Money`, and why does it matter?
3. Why enforce invariants in the `Money` constructor instead of in the controller?
4. If a `@RestControllerAdvice` catches `Exception.class`, what should you be
   careful *not* to put in the response body? Why?

## 7. Principal Engineer review checklist

- [ ] Can I explain the dependency rule in one sentence?
- [ ] Do I understand *why* `Money` is immutable?
- [ ] Do I know what changes when we swap in-memory → PostgreSQL (which files, which don't)?
- [ ] Have I completed at least 3 of the exercises above?
