# Guide Notes — How we work together

> This folder is **guidance only**. The actual project code lives in the parent
> folder (`Real-Time Payment/`), not here.

## Our working agreement

- **You implement. I guide and review.** I explain the *why*, give you a plan
  and hints, and review your work like a Principal Engineer. I will **not**
  write the production code for you.
- **One small milestone at a time.** Each milestone is sized for ~1 hour/day.
- **Run and verify before moving on.** Code that isn't run and tested is a
  guess, not an answer.

## The rhythm for every milestone

1. Read the milestone note in this folder.
2. Study the "Concepts to understand" section (What / Why / How / trade-offs).
3. Do the implementation tasks **yourself**.
4. Run the tests and the service. Verify against the checklist.
5. Bring your work back for review. We discuss trade-offs like engineers.

## Notes index

| Note | Topic | Status |
|------|-------|--------|
| [milestone-01-payment-service-foundation.md](milestone-01-payment-service-foundation.md) | Review the foundation (already scaffolded) + exercises | ✅ Ready |
| [milestone-02-postgresql-flyway.md](milestone-02-postgresql-flyway.md) | Add PostgreSQL + Flyway — **you implement this** | ▶ Next up |
| [how-to-open-and-build-in-intellij.md](how-to-open-and-build-in-intellij.md) | Open & build the project in IntelliJ IDEA Community | 🛠 Setup |
| [beginner-guide-flow-and-structure.md](beginner-guide-flow-and-structure.md) | Beginner's walkthrough of the project's flow & structure | 📖 Read first |
| [architecture-and-planning.md](architecture-and-planning.md) | Architecture & roadmap explained in plain English | 🗺 Big picture |

## Where the code lives

| What | Path |
|------|------|
| Platform root | `../` |
| Payment service | `../payment-service/` |
| Architecture docs | `../docs/` |
| ADRs | `../docs/adr/` |
