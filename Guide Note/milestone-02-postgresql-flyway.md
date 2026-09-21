# Milestone 02 — PostgreSQL + Flyway (you implement this)

> **Goal:** replace the in-memory store with PostgreSQL, and manage the schema
> with Flyway migrations. This is the first time *you* write the code.

## 1. The business/technical problem

An in-memory store dies when the process stops. Real payments must **survive
restarts** and be queryable, indexed, and consistent. So we persist `Payment`
to PostgreSQL — the service's **own** database (database-per-service principle:
no other service touches this database).

## 2. Concepts to understand first

1. **Flyway migrations** — versioned SQL files (`V1__…`, `V2__…`) applied in
   order. The schema is *code*, version-controlled, and repeatable on any
   environment. (Alternative: Liquibase. We use Flyway.)
2. **JPA entity vs domain model** — see the design decision below. This is the
   most important thinking in this milestone.
3. **Repository pattern in Spring Data** — `JpaRepository` gives you CRUD for
   free, but you still wrap it behind your `PaymentRepository` **port** so the
   domain/application layers stay framework-free.
4. **Connection pool** — HikariCP (Spring Boot default). Later milestones cover
   pool sizing and exhaustion; for now just know it exists.

## 3. Your tasks (do these in order)

### Task 1 — Add dependencies to `payment-service/pom.xml`

You need:
- `spring-boot-starter-data-jpa`
- `flyway-core` **and** `flyway-database-postgresql` (Flyway 10+ split DB support)
- `org.postgresql:postgresql` (runtime)
- Test-only: `org.testcontainers:junit-jupiter`, `org.testcontainers:postgresql`,
  `org.springframework.boot:spring-boot-testcontainers`

> Hint: let the Spring Boot parent manage versions — omit `<version>` for these.

### Task 2 — Write the Flyway migration

Create `payment-service/src/main/resources/db/migration/V1__create_payments_table.sql`.

Design the `payments` table yourself. It must store:
- `payment_id` (PK, the value we generate as `PAY-…`)
- `source_account_id`, `destination_account_id`
- `amount` as `NUMERIC(19,4)` (never `FLOAT`/`DOUBLE` for money — why?)
- `currency` as `CHAR(3)`
- `status` as `VARCHAR`
- `created_at` as `TIMESTAMPTZ`
- `version` — an integer for **optimistic locking** (used in a later milestone)

Add sensible `NOT NULL` constraints and a check constraint on `status`.

### Task 3 — Make the design decision (think, then choose)

Two options for mapping the domain `Payment` to the database:

- **Option A — annotate the domain class directly.** Add `@Entity`, `@Id`,
  `@Version`, a `protected` no-arg constructor, non-final fields, and getters.
  *Pragmatic, common, fewer classes — but it couples the domain to Hibernate.*
- **Option B — separate persistence model.** Keep `domain/model/Payment.java`
  pure, and create a `PaymentEntity` (JPA) in `adapter/out/persistence` plus a
  small mapper between them. *More files, but the domain stays clean and the
  hexagonal boundary holds.*

**Write down your choice and your reasoning (2–3 sentences) before you code.**
I recommend Option B for this project — it demonstrates *why* we built ports
& adapters. Bring your reasoning to review.

### Task 4 — Implement the JPA repository adapter

Create the adapter (e.g. `adapter/out/persistence/JpaPaymentRepository`) that
implements the existing `application/PaymentRepository` port, using a Spring
Data `JpaRepository` internally. You'll need the mapper from Task 3.

**Watch out:** if both `InMemoryPaymentRepository` and the JPA one are `@Repository`
beans, Spring will fail with "expected single bean." Decide how to handle it —
e.g. delete the in-memory one, or mark it `@Profile("!postgres")` / `@ConditionalOnProperty`.
Justify your choice.

### Task 5 — Configure the datasource

In `application.yml`, add the `spring.datasource.*`, `spring.jpa.*`, and
`spring.flyway.*` settings. **Use environment variables for credentials** — never
hard-code a password:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
```

Decide what `spring.jpa.hibernate.ddl-auto` should be now that Flyway owns the
schema. (Think: who should be the source of truth?)

### Task 6 — Run PostgreSQL locally

You have Docker available. Start a PostgreSQL container (pick your own db/name):

```bash
docker run --name payments-db -e POSTGRES_USER=... -e POSTGRES_PASSWORD=... \
  -e POSTGRES_DB=paymentdb -p 5432:5432 -d postgres:16
```

Then run `./mvnw spring-boot:run` and repeat the curl checks from Milestone 1.
Confirm the payment **survives a restart** (restart the app, GET the same id).

### Task 7 — Test it

- Your existing domain test (`PaymentTest`) should still pass **unchanged** —
  that's the proof the domain didn't leak.
- Add an **integration test** with Testcontainers that writes and reads a
  payment against a real PostgreSQL. This also proves your Flyway migration runs.
- Decide what happens to `PaymentServiceApplicationTest` (the context-load test):
  now that JPA is on the classpath, does it need a real database? How will you
  provide one? (Hint: Testcontainers `@ServiceConnection`.)

## 4. Gotchas to watch for

- **Money precision** — use `BigDecimal`/`NUMERIC`, never `double`.
- **Flyway + `ddl-auto`** — if Hibernate also tries to create tables, they fight.
  Pick one owner of the schema.
- **Enum mapping** — store `status` as a string; don't rely on `EnumType.ORDINAL`
  (reordering enums would corrupt data).
- **The context-load test now needs a DB** — don't ship a test that only passes
  on your machine.

## 5. Verification checklist

- [ ] `./mvnw test` passes (old tests + new Testcontainers test).
- [ ] `docker exec` into the DB confirms the `payments` table exists (created by Flyway).
- [ ] A created payment survives an app restart.
- [ ] `domain/model/Payment.java` has **no** JPA/Hibernate imports (if Option B).
- [ ] No hard-coded credentials anywhere.

## 6. Interview questions (after you finish)

1. Why did you choose Option A or B? What is the cost of the choice you made?
2. Who owns the database schema — Flyway or Hibernate? Why does that matter?
3. Why `NUMERIC(19,4)` and not `DOUBLE PRECISION` for money?
4. What does the `version` column do, and when will we actually use it?

