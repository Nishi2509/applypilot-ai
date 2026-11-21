# Database migrations

The Spring Boot service owns schema migrations. Flyway scripts live in
`backend/src/main/resources/db/migration` and are applied automatically when the
backend starts. This directory is reserved for database operational material.
