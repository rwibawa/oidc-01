Yes. At a high level, your app becomes an OpenID Connect client, Google acts as the Identity Provider / Authorization Server, and OAuth 2.0 handles authorization/token issuance.

Google's OAuth 2.0 implementation supports OpenID Connect for authentication, and Google publishes a standard OIDC discovery document for endpoints and metadata. Google OpenID Connect

Since you work with Java/Spring Boot, here is a practical Spring Boot + React-style architecture.

1. Architecture
                    Google Identity
                OIDC / OAuth2 Provider
                         |
                         |
              Authorization Code + PKCE
                         |
                         v
+------------+       +--------------------+
|            |       |                    |
| React UI   |------>| Spring Boot App    |
|            |       | OAuth2/OIDC Client |
+------------+       +--------------------+
                              |
                              |
                         User Session
                              |
                              v
                    +-------------------+
                    | Application APIs  |
                    +-------------------+
                              |
                              v
                         PostgreSQL


Think of the responsibilities as:

OpenID Connect: authenticates the user.
OAuth 2.0: authorizes access to protected resources/API scopes.
Google: Identity Provider.
ID token: identity information about the authenticated user.
Access token: used to call the authorized protected API.
Your application: establishes its own authenticated session after successful login.

Google's ID token is a signed JWT; Google's OIDC metadata specifies things such as the issuer, authorization endpoint, token endpoint, userinfo endpoint, and JWKS endpoint. Google OIDC API Reference

2. Create the Google application

Go to the Google Cloud Console.

Create/select a project and configure the OAuth consent screen.

Then create:

OAuth Client ID → Web application

You'll receive:

Client ID
xxxxxxxx.apps.googleusercontent.com

Client Secret
GOCSPX-xxxxxxxx


For Spring Security, you could use a callback such as:

http://localhost:8080/login/oauth2/code/google


and production:

https://myapp.example.com/login/oauth2/code/google


The redirect URI registered with Google must correspond to the application's callback URI. Google's setup requires OAuth credentials and a configured redirect URI. Google OpenID Connect

3. Spring Boot setup

For a Maven application:

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>


The important piece is:

spring-boot-starter-oauth2-client

4. Configure Google OIDC

application.yml

spring:

  security:

    oauth2:

      client:

        registration:

          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}

            scope:
              - openid
              - profile
              - email


Set secrets through environment variables:

export GOOGLE_CLIENT_ID="xxxx.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="xxxx"


Do not put the client secret in React/JavaScript or commit it to source control.

With OIDC you normally want:

openid
profile
email


The openid scope is what turns the OAuth authorization request into an OpenID Connect authentication request.

5. Spring Security

Example configuration:

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/login/**",
                    "/public/**"
                ).permitAll()

                .requestMatchers("/api/**")
                .authenticated()

                .anyRequest()
                .authenticated()
            )

            .oauth2Login(Customizer.withDefaults());

        return http.build();
    }
}


The key:

.oauth2Login(Customizer.withDefaults())


activates the OAuth/OIDC login flow.

6. Add a Login button

Your React application doesn't need to collect Google credentials.

Simply redirect the browser to Spring Security:

function Login() {

    const loginWithGoogle = () => {
        window.location.href =
            "http://localhost:8080/oauth2/authorization/google";
    };

    return (
        <button onClick={loginWithGoogle}>
            Sign in with Google
        </button>
    );
}


Request:

GET /oauth2/authorization/google


Spring Security initiates the authorization flow.

Conceptually:

React
   |
   v
Spring Boot
   |
   | redirect
   v
Google
   |
   | login/consent
   v
Google
   |
   | authorization code
   v
Spring Boot
   |
   | exchange code
   v
Google Token Endpoint
   |
   +---- ID Token
   |
   +---- Access Token


Google documents this general server authentication flow and token validation model. Google OpenID Connect

7. Read the authenticated user

Now create an endpoint:

@RestController
@RequestMapping("/api")
public class UserController {

    @GetMapping("/me")
    public Map<String, Object> me(
            @AuthenticationPrincipal OidcUser user) {

        return Map.of(
            "subject", user.getSubject(),
            "name", user.getFullName(),
            "email", user.getEmail()
        );
    }
}


React can call:

GET /api/me


and receive something like:

{
  "subject": "10394829384928394829",
  "name": "Ryan Smith",
  "email": "ryan@example.com"
}


The sub/subject identifier is particularly important. Avoid using email alone as your application's immutable user identifier.

8. What actually happens during login

The protocol is the most useful part to understand.

Step 1: User clicks login
Sign in with Google


Browser goes to:

/oauth2/authorization/google

Step 2: Authorization request

Your application sends the browser to Google's authorization endpoint with information conceptually equivalent to:

client_id=...
response_type=code
scope=openid profile email
redirect_uri=https://...
state=...


For flows using PKCE, you'll also see PKCE-related parameters.

Step 3: Google authenticates the user

Google handles:

username
password
MFA
account selection
consent


Your application should never receive the user's Google password.

Step 4: Google redirects back

Conceptually:

GET /login/oauth2/code/google
    ?code=ABC123
    &state=XYZ


The authorization code is short-lived and intended for the token exchange.

Step 5: Backend exchanges the code

Spring communicates with Google's token endpoint.

Conceptually:

POST Google Token Endpoint

code=ABC123
client_id=...
client_secret=...
redirect_uri=...
grant_type=authorization_code

Step 6: Tokens are returned

Conceptually:

{
  "access_token": "...",
  "id_token": "...",
  "expires_in": 3600,
  "token_type": "Bearer"
}


The exact response depends on the request and authorization circumstances.

9. Understand ID token vs access token

This distinction is extremely important.

ID Token

OIDC:

ID Token

       |
       v

Who is this person?


JWT payload contains identity claims such as:

{
  "iss": "https://accounts.google.com",
  "sub": "123456789",
  "email": "user@example.com",
  "name": "Ryan Smith"
}


Use the ID token for authentication/identity.

Google specifies that the ID token must be verified using Google's published key material. Google OIDC API Reference

Access Token

OAuth:

Access Token

       |
       v

What resource may this application access?


For example, if your app requested and received appropriate Google API permissions, the access token could be used against those APIs.

Don't use the access token as your application's proof of the user's identity. That's what OIDC and the ID token are for.

10. Google Discovery endpoint

One of the best things about OIDC is discovery.

Google publishes:

https://accounts.google.com/.well-known/openid-configuration


The discovery document exposes configuration including:

issuer
authorization_endpoint
token_endpoint
userinfo_endpoint
jwks_uri
scopes_supported
claims_supported


Google documents this discovery URL and its metadata fields directly. Google OIDC API Reference

That means an OIDC implementation doesn't need you to hard-code every Google endpoint manually.

11. If you have React + Spring Boot APIs

For a normal business web application, I'd recommend a Backend-for-Frontend/session-oriented design:

Browser / React
       |
       | HTTPS
       v
+--------------------+
| Spring Boot        |
|                    |
| OAuth2/OIDC Client |
|                    |
| Session Cookie     |
+----------+---------+
           |
           | OIDC
           v
+--------------------+
| Google Identity    |
+--------------------+


Rather than:

React
   |
   +--- storing Google tokens in localStorage


In particular, I would avoid putting refresh tokens or client secrets into browser-accessible storage.

12. Production version

A more complete architecture could look like:

                    Internet

                       |
                       v
                 CDN / WAF
                       |
                       v
             +------------------+
             | React Application|
             +--------+---------+
                      |
                      | HTTPS
                      v
             +------------------+
             | API Gateway / ALB|
             +--------+---------+
                      |
                      v
             +------------------+
             | Spring Boot      |
             |                  |
             | OIDC Client      |
             | Spring Security  |
             +--------+---------+
                      |
          +-----------+-----------+
          |                       |
          v                       v
   Google Identity          PostgreSQL
   OIDC Provider



For your AWS/EKS architecture interests, Spring Boot can live behind an ALB/Ingress and the React build can sit behind CloudFront, while Google remains the external IdP.

13. The key security concepts

Your application should account for:

State

Protects the authorization redirect against request-forgery/login-flow attacks.

Nonce

Associates the ID token with the authentication request and helps prevent token replay/substitution scenarios.

PKCE

Binds the authorization request to the subsequent authorization-code exchange.

ID-token validation

Validate the token rather than just decoding the JWT. Google's documentation specifically says the returned signed ID token must be verified using the key material from its jwks_uri. Google OIDC API Reference

HTTPS

Use HTTPS in production.

Secrets

Keep:

GOOGLE_CLIENT_SECRET


server-side.

Recommended project structure

For something production-oriented, I'd structure your application roughly like:

myapp/

├── frontend/
│   ├── src/
│   │   ├── auth/
│   │   │   ├── Login.tsx
│   │   │   └── AuthContext.tsx
│   │   ├── api/
│   │   │   └── client.ts
│   │   └── App.tsx
│   └── package.json
│
├── backend/
│   ├── src/main/java/com/example/app/
│   │
│   ├── config/
│   │   └── SecurityConfig.java
│   │
│   ├── auth/
│   │   ├── UserController.java
│   │   └── CustomOidcUserService.java
│   │
│   ├── user/
│   │   ├── User.java
│   │   ├── UserRepository.java
│   │   └── UserService.java
│   │
│   └── Application.java
│
├── Dockerfile
└── docker-compose.yml


The important mental model is:

Google authenticates the person → OIDC tells Spring who that person is → your application maps Google's sub to an internal user → your own authorization rules determine what that user can do.

For a React + Spring Boot app, this separation gives you a clean path later to add Microsoft Entra ID, Okta, Auth0, or another OIDC provider without redesigning your application's entire authentication model.