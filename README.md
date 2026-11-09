# Google OIDC sign-in

A minimal session-based OpenID Connect application. Spring Boot handles the
Google authorization-code flow and keeps the authenticated session on the
server; the React UI never receives Google tokens or the client secret.

## Requirements

- Java 21
- Maven 3.9+
- Node.js 20.19+ or 22.12+
- A Google OAuth 2.0 Web application client

Add `http://localhost:8080/login/oauth2/code/google` as an authorized redirect
URI in the Google client configuration.

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

## API

- `GET /api/csrf` returns the CSRF header name and token for the current session.
- `GET /api/me` returns the authenticated user's OIDC subject, name, and email;
  it returns `401` when the browser has no authenticated session.
- `POST /api/logout` invalidates the session and requires the CSRF header
  returned by `/api/csrf`.

The OIDC subject (`sub`) is exposed as the stable account identifier; email is
profile information and should not be used as the user key.

## Production notes

Serve the built React app and proxy `/api/**`, `/oauth2/**`, and `/login/**` to
Spring Boot under the same HTTPS origin. Set `APP_FRONTEND_URL` to that public
origin and `SESSION_COOKIE_SECURE=true`. Keep the OAuth client secret in the
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
