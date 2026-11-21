# API documentation

All API routes are rooted at `/api/v1`. The live OpenAPI UI is planned at
`/swagger-ui.html` once the broader API surface is implemented.

| Method | Route | Authentication | Current behavior |
| --- | --- | --- | --- |
| POST | `/auth/register` | No | Creates an account and returns a JWT. |
| POST | `/auth/login` | No | Authenticates credentials and returns a JWT. |
| GET | `/users/me` | Bearer JWT | Returns the authenticated account. |
| GET | `/users/preferences` | Bearer JWT | Returns job-search preferences. |
| PUT | `/users/preferences` | Bearer JWT | Replaces validated preferences. |
| GET | `/dashboard/summary` | Bearer JWT | Returns persisted dashboard counts. |
| GET | `/jobs` | Bearer JWT | Lists jobs already persisted by permitted sources. |
| GET | `/jobs/{id}` | Bearer JWT | Returns a persisted job detail. |
| GET | `/integrations/status` | Bearer JWT | Returns configuration and OAuth connection state. |
| POST | `/integrations/gmail/connect` | Bearer JWT | Creates a short-lived Google OAuth authorization URL. |
| POST | `/integrations/google-sheets/connect` | Bearer JWT | Creates the same consolidated Google OAuth authorization URL. |
| GET | `/integrations/google/callback` | Google redirect | Exchanges an authorization code and redirects to the UI. |

Google OAuth state is one-time, hashed and expires after ten minutes. Access and
refresh tokens are encrypted with AES-GCM before persistence. The OAuth callback
is the only public integration route; it identifies the requesting user through
the stored, validated OAuth state.

Later endpoints listed in the project requirements retain this base path. Long
operations will return `202 Accepted` with a persistent task identifier. Errors
use RFC 9457-style problem JSON with a timestamp and validation detail.
