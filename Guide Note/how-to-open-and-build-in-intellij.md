# How to open & build the project in IntelliJ IDEA (Community)

> Applies to the free **IntelliJ IDEA Community Edition**. You already have
> IntelliJ IDEA **2026.2.3** installed (via snap) and **JDK 21** at
> `/usr/lib/jvm/java-21-openjdk-amd64`. Community Edition is fully sufficient
> for this project.

## 1. Launch IntelliJ

- Open your app launcher (Super/Windows key) and search **"IntelliJ IDEA"**, or
- In a terminal: `intellij-idea` (the command may be `intellij-idea-community`
  depending on how it was installed — the app launcher is the reliable way).

## 2. Open the project

1. **File → Open…** (or "Open" on the welcome screen).
2. Select the platform root: `~/Project/Real-Time Payment`.
   - Open the *root* (not just `payment-service`) so you can also see `docs/`
     and `Guide Note/` in the Project view.
3. IntelliJ asks **"Trust Project?"** → click **Trust**.
4. It detects the Maven project in `payment-service/pom.xml` → click
   **"Load Maven Project"** (or the "Load Maven Changes" prompt that pops up).
   - If it doesn't auto-detect, open `payment-service/pom.xml` and look for a
     small **"Add as Maven Project"** prompt in the top-right.

## 3. Set the JDK (one-time)

1. **File → Project Structure → Project** (or `Ctrl+Shift+Alt+S`).
2. Set **SDK** to **21** (`java-21-openjdk-amd64`).
3. **Language level** should read **17** — IntelliJ usually picks this up from
   the `pom.xml`.

> **Why 21 but targeting 17?** You develop on JDK 21, but the `pom.xml` sets
> `java.version=17`, which makes Maven compile with `--release 17`. So the
> shipped bytecode is Java-17-compatible — which is what enterprise banking
> actually runs.

## 4. Use the Maven wrapper (recommended, one-time)

1. **Settings → Build, Execution, Deployment → Build Tools → Maven**.
2. Set **Maven home path** to **"Use Maven wrapper"** (`./mvnw`).

> Why: the wrapper pins Maven 3.9.16 for everyone, so the build is identical on
> your machine, CI, and a teammate's machine. No "works on my machine" drift.

## 5. Build & run tests

- **Maven tool window** (right sidebar; if hidden: **View → Tool Windows → Maven**)
  → `payment-service` → **Lifecycle** → double-click `test` (or `package`).
- Or in the terminal (**View → Tool Windows → Terminal**, `Alt+F12`):
  ```bash
  cd payment-service
  ./mvnw test
  ```
- Quick compile only: **Build → Build Project** (`Ctrl+F9`).

## 6. Run the application

- Easiest: open
  `payment-service/.../paymentservice/PaymentServiceApplication.java` and click
  the **green run arrow** next to `main` (or `Shift+F10`).
- Or via Maven: Maven tool window → **Plugins → spring-boot → spring-boot:run**.

Then verify: http://localhost:8081/actuator/health should return `{"status":"UP"}`.

## 7. Run a single test

- Click the **green arrow in the gutter** next to a test method/class → **Run**,
- or right-click a test class in the Project view → **Run '…Test'**.

## 8. Community vs Ultimate (what you DON'T have, and the workaround)

The free Community Edition lacks Spring-specific extras. You can live without them:

| Ultimate-only feature | Workaround in Community |
|-----------------------|-------------------------|
| Spring Boot run dashboard | Run the `main` method directly (works fine) |
| Spring-specific run configs | Same — run `PaymentServiceApplication` |
| Built-in HTTP client | Use `curl` or Postman |
| Spring bean/mapping diagrams | Maven tool window + terminal |

## 9. Common issues & fixes

| Symptom | Fix |
|---------|-----|
| "Maven projects need to be imported" | Click **Load Maven Changes** |
| "Cannot resolve symbol" / red code | Reload Maven project (Maven tool window → refresh icon) |
| "Project JDK is not defined" | Set SDK in **Project Structure** (step 3) |
| Milestone 2+ tests fail with "Could not find a valid Docker environment" | Start Docker; Testcontainers needs it |
| First `./mvnw` run is slow | It's downloading Maven once; subsequent runs are cached |
| `Port 8081 already in use` | Stop the old app instance (or `kill` the java process) |

## 10. Useful shortcuts

| Action | Shortcut |
|--------|----------|
| Build project | `Ctrl+F9` |
| Run current class/test | `Shift+F10` |
| Run context menu | `Alt+Shift+F10` |
| Open terminal | `Alt+F12` |
| Project Structure | `Ctrl+Shift+Alt+S` |
| Search file | `Ctrl+Shift+N` (or double `Shift`) |
