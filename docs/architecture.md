# Architecture

ApplyPilot AI is a monorepo with three deployable applications:

```text
Next.js dashboard ─REST/JWT─> Spring Boot API ─JPA/Flyway─> MySQL
                                  │
                                  └─HTTP (private network)─> FastAPI AI service
```

The Spring Boot API is the workflow authority and the only service that writes
application state, invokes Gmail/Sheets, or schedules work. The AI service
returns validated structured analysis and never accesses OAuth tokens, sends
mail, or writes Sheets. MySQL is the source of truth; Google Sheets is a
best-effort synchronized report.

### Security boundaries

The browser receives a short-lived JWT only after password authentication.
Passwords use BCrypt. File uploads and future job-source content are untrusted;
they will be size/type limited, stored privately, and never executed. The
approval state and email/version checks will be enforced transactionally by the
backend, rather than trusted to the UI.

### Current foundation

The backend exposes `/actuator/health` and versioned auth/preferences endpoints.
The AI service exposes `/health` and a deterministic matching endpoint suitable
for service-to-service smoke checks. The frontend provides protected navigation
and empty states; it does not display invented application metrics.
