# Google OIDC sign-in

A session-based OpenID Connect application with persistent internal users. Spring Boot handles the
Google authorization-code flow and keeps the authenticated session on the
server; the React UI never receives Google tokens or the client secret.

## Requirements

- Java 25
- Maven 3.9+
- Node.js 20.19+ or 22.12+
- A Google OAuth 2.0 Web application client

Add `http://localhost:5173/login/oauth2/code/google` as an authorized redirect
URI in the Google client configuration for the proxied frontend. Also register
`http://localhost:8080/login/oauth2/code/google` if initiating login directly
through the backend.

## Configure credentials

Set these variables in the shell that runs the backend:

```powershell
$env:GOOGLE_CLIENT_ID = "your-client-id.apps.googleusercontent.com"
$env:GOOGLE_CLIENT_SECRET = "your-client-secret"
```

The client secret must remain server-side and must not be committed or added to
the frontend.

## Run locally

In one terminal:

```powershell
cd backend
mvn spring-boot:run
```

In another terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open the Vite URL (normally `http://localhost:5173`). The Vite development
server proxies API and OAuth routes to Spring Boot. After Google signs the user
in, Spring redirects to `APP_FRONTEND_URL`, which defaults to the Vite URL.

## Internal users and database profiles

On the first successful Google login, the backend creates an internal user with
a generated UUID. Later logins update the name and email while preserving the
UUID and creation timestamp. The unique identity key is `(issuer, subject)`;
email changes do not create a new account, and shared email addresses do not
merge accounts. Any Google user allowed by your OAuth consent configuration can
sign in; this is automatic provisioning, not an employee allowlist or admin API.
Database failures prevent login rather than creating a non-persistent session.

| Spring profile | Database | Configuration |
| --- | --- | --- |
| `dev` (default) | Persistent H2 | `backend/data/oidc` when launched from `backend` |
| `qa` | PostgreSQL | `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` |
| `staging` | PostgreSQL | Same required variables |
| `production` | PostgreSQL | Same required variables |

For example, to run against an existing PostgreSQL database:

```powershell
$env:SPRING_PROFILES_ACTIVE = "qa"
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/oidc"
$env:DATABASE_USERNAME = "oidc"
$env:DATABASE_PASSWORD = "your-database-password"
cd backend
mvn spring-boot:run
```

Use exactly one environment profile. PostgreSQL profiles have no H2 fallback and
require an existing database and credentials with permission to apply migrations.
Flyway creates and versions the schema on both database engines; Hibernate only
validates it. Add future schema changes as new migrations rather than modifying
an already-applied migration. The H2 console is not exposed.

Optional dev overrides: `DEV_DATABASE_URL` (a JDBC H2 URL) and
`DEV_DATABASE_PASSWORD`. The default H2 username is `sa`; local database files
are git-ignored.

## Local development with Docker Compose

Start Docker Desktop with the Linux container engine, then from the repository root:

```powershell
Copy-Item .env.example .env
# Edit .env to supply your Google OAuth client ID and secret.
docker compose up --build
```

Open `http://localhost:5173`. Compose runs the backend with the `dev` profile and
H2, and the frontend with Vite. OAuth and API requests are proxied to the backend;
register the `5173` callback URI described above. No PostgreSQL server is needed
for dev. Both published ports bind only to localhost.

The named `backend-data` volume preserves H2 users across container recreation
and `docker compose down`. Frontend source and HTML are bind-mounted for live
updates. Rebuild after backend changes with `docker compose up --build backend`,
and rebuild the frontend after dependency or Vite configuration changes.
Keep `DEV_DATABASE_PASSWORD` stable after the database is first created.

```powershell
docker compose logs -f
docker compose down
```

`docker compose down --volumes` additionally deletes the local database; use it
only when intentionally resetting dev data.

Both Dockerfiles also provide production runtime images:

```powershell
docker build -t oidc-backend .\backend
docker build -t oidc-frontend .\frontend
```

The backend runs as a non-root Java 25 process on port 8080. The frontend's
default final stage serves the compiled SPA through unprivileged Nginx on port
8080 and proxies to `backend:8080`; use that backend DNS name on their shared
container network, or adapt `frontend/nginx.conf` for your deployment.
The Compose file explicitly selects the frontend `dev` target instead.
Neither build requires or embeds OAuth or database credentials.

## API

- `GET /api/csrf` returns the CSRF header name and token for the current session.
- `GET /api/me` returns the persisted internal user's `id` (UUID), OIDC
  `subject`, `name`, and nullable `email`; it returns `401` when there is no
  authenticated session or its internal user no longer exists.
- `POST /api/logout` invalidates the session and requires the CSRF header
  returned by `/api/csrf`.

The internal UUID is the application account identifier. The OIDC subject
(`sub`) remains exposed for compatibility; email is profile information and
should not be used as the user key. Existing sessions from before user
persistence was added must sign in again to provision their internal account.

## Production notes

Serve the built React app and proxy `/api/**`, `/oauth2/**`, and `/login/**` to
Spring Boot under the same HTTPS origin. Set `APP_FRONTEND_URL` to that public
origin and `SESSION_COOKIE_SECURE=true`, and activate `production` with the
required PostgreSQL variables. Register the public `/login/oauth2/code/google`
callback with Google. When TLS terminates at a trusted reverse proxy, set
`SERVER_FORWARD_HEADERS_STRATEGY=framework` and ensure the backend is reachable
only through that proxy, which must overwrite forwarded headers.
Keep the OAuth client secret in the
deployment secret store. The session cookie is HTTP-only and uses `SameSite=Lax`.

## Verify

```powershell
cd frontend
npm test
npm run build
```

```powershell
cd backend
mvn test
```
