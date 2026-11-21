# Implementation plan

## Milestone 1 — foundation (implemented)

- Create the Next.js, Spring Boot and FastAPI applications.
- Run MySQL through Docker and create an auditable Flyway schema.
- Implement JWT-backed registration, login, identity lookup and editable user
  preferences. New accounts receive the requested job-search defaults.
- Add health endpoints, environment configuration, container definitions and
  smoke/unit tests.

## Milestone 2 — job discovery

Define the `JobSourceProvider` contract, then add Greenhouse and Lever adapters
only through their public job-board interfaces. Persist normalized jobs with
source/external-id de-duplication and build the job explorer.

## Milestone 3 — analysis and matching

Implement validated JD/resume parsing, deterministic weighted scoring and the
backend-to-AI-service client. LLM output will supply structured evidence only;
the Java/Python scoring code calculates every score.

## Milestones 4–8

Resume ingestion and safe isolated LaTeX compilation; contacts, drafts and
approval-gated Gmail sending; Sheets synchronization; dashboard/scheduling;
then security review, test expansion and CI/CD. Each milestone remains
independently deployable and tested before the next begins.

## Decisions and prerequisites

Java 21, MySQL 8.4 LTS, Python 3.12 and Node 22 LTS are required locally or
through Docker. OAuth client credentials and an LLM key are intentionally not
required for Milestone 1; `.env.example` records them for later milestones.
