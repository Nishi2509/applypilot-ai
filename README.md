# ApplyPilot AI

An approval-first job discovery, resume customization and application-tracking
system. It is a monorepo for a Next.js dashboard, Spring Boot workflow API,
FastAPI analysis service and MySQL source of truth.

## Start locally

1. Install Docker Desktop and create `.env` from `.env.example`.
2. Set a unique, high-entropy `JWT_SECRET` (at least 32 bytes).
3. Run `docker compose up --build`.
4. Open `http://localhost:3000`; API health is at
   `http://localhost:8080/actuator/health`; AI health is at
   `http://localhost:8000/health`.

MySQL is exposed to the host on port `3307` by default, avoiding a conflict
with a locally installed MySQL instance on `3306`. Containers connect to MySQL
internally on port `3306`.

For direct development, use Java 21/Maven, Python 3.12 and Node 22. Run
`mvn test` in `backend`, `pytest` in `ai-service`, and `npm run lint` in
`frontend` after installing each service's dependencies.

No application email can be sent by scheduled or automatic processing. Future
Gmail sending is explicitly approval-gated and will only report success after
Gmail confirms it.

## Master resume

In **Resume Studio**, upload both your recruiter-ready PDF and its original
LaTeX `.tex` source. The application stores each submission as an immutable,
user-owned version under `storage/resumes/` (mounted persistently into the
backend container). When an application does not need a job-specific resume,
the latest master PDF is the default attachment candidate; an application is
still never emailed until it is explicitly approved.

## Manual job discovery

Open **Job Explorer**, add the company name and board identifier from an
official Greenhouse or Lever careers URL, then select **Search jobs now**. For
example, the identifier in `https://jobs.lever.co/acme` is `acme`. ApplyPilot
fetches only the board's published roles and stores the job title, company,
description, location, source link, and direct application URL. The search is
manual; no scheduler, job-board scraping, or automatic application/email is
used. Naukri, LinkedIn, and other job boards require their own approved or
licensed API integration before they can be added.

### Optional web search

The **Search web** button uses the OpenAI Responses API web-search tool when
`OPENAI_API_KEY` is present (it is not sent to the browser). Set an optional
model override only when your OpenAI project requires one:

```text
OPENAI_API_KEY=...
OPENAI_WEB_SEARCH_MODEL=gpt-5.4
```

Restart the backend after changing `.env`. The button sends up to three queries
derived from your saved roles and first preferred location to OpenAI web search,
and stores cited result titles, snippets, and links as job leads. `TAVILY_API_KEY`
remains an optional fallback when no OpenAI key is configured. It does not scrape
Google, Naukri, LinkedIn, or search-result pages.

## Google OAuth setup

The backend owns the OAuth callback so browser code never handles the Google
client secret or refresh token. Add these values to your local `.env` before
connecting Gmail or Google Sheets:

```text
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
GOOGLE_REDIRECT_URI=http://localhost:8080/api/v1/integrations/google/callback
APP_FRONTEND_BASE_URL=http://localhost:3001
APP_TOKEN_ENCRYPTION_KEY=<Base64-encoded 32-byte key>
```

Generate the encryption key without committing it:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

If Docker MySQL was initialized with an older password, changing
`MYSQL_PASSWORD` in `.env` does not change the database account inside its
persistent volume. Use the original password for `SPRING_DATASOURCE_PASSWORD`,
or deliberately reset/reconfigure the local volume after backing up data.

See [the implementation plan](docs/implementation-plan.md) for current scope.
