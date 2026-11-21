# Database design

Flyway migration `V1__initial_schema.sql` creates the normalized core schema.
All primary IDs are UUIDs, UTC audit timestamps are database-managed, and JSON
is used only for structured evidence/configuration.

| Area | Key rules |
| --- | --- |
| Identity | `users.email` is unique; password hashes only are persisted. |
| Jobs | `(source, external_job_id)` is unique; a fingerprint supports sources without an ID. |
| Resumes | master file hashes identify immutable uploads; `(job_id, version_number)` is unique. |
| Applications | `(user_id, job_id)` is unique; sending uses idempotency keys. |
| Approvals | approval binds an email version and custom-resume version. |
| OAuth | encrypted token columns are reserved; plaintext tokens are never persisted. |

`ddl-auto=validate` is used in the backend. Schema changes must be additive,
reviewed Flyway migrations; Hibernate must not mutate production schemas.
