# Architecture Overview

## Layered (Hexagonal) structure

Each service uses **hexagonal architecture** (ports & adapters) so that the
business logic is isolated from infrastructure (HTTP, databases, messaging).

```
        adapter/in/rest  (driving adapter: HTTP controllers)
                |
                v
         application      (use cases + ports)
                |
                v
          domain          (entities, value objects, invariants)
                ^
                |
        adapter/out/*     (driven adapters: persistence, messaging, ...)
```

The dependency rule: **domain does not depend on anything. Application depends
on domain. Adapters depend on application/domain — never the other way around.**

## Why this structure?

1. **Testability** — the domain and application layers can be unit-tested with
   no Spring, no database, no HTTP.
2. **Replaceability** — the in-memory store can be swapped for PostgreSQL
   without touching business logic.
3. **Clarity** — a new engineer can see where a change belongs.

## What it is NOT

We do not add an extra interface "for the future" when there is only one
implementation. Each layer exists because it earns its place.
