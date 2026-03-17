# Security Implementation Notes

This document explains the security design in this project in a "how it works" way, not just a list of classes.

## 1. Big Picture
The API uses stateless authentication with JWT.

What that means in practice:
- The server does not keep login sessions in memory.
- Each protected request must include a Bearer access token.
- Authorization decisions are based on user roles loaded from DB (`USER`, `ADMIN`).

This is different from classic stateful login (JSESSIONID cookie + server-side session), and it is why the configuration disables session creation.

## 2. Why This Is Different From Class Baseline
In class, a common setup uses `httpBasic()` and a straightforward custom filter. That is useful to learn the internals, but for this project we made a few production-oriented decisions:

- Removed `httpBasic()`:
  Basic auth sends username/password repeatedly on every request. JWT avoids that and supports token expiry/rotation.

- Added `AuthenticationProvider` and `AuthenticationManager` beans:
  We still need secure username/password verification during login. This is delegated to Spring Security (`DaoAuthenticationProvider`) instead of custom manual checks.

- Added explicit access-token vs refresh-token separation:
  A `token_type` claim is used (`access` or `refresh`). Access tokens are accepted by request filter; refresh tokens are accepted only by `/api/auth/refresh`.

- Added role-based route protection:
  `/api/admin/**` requires `ADMIN`. Regular endpoints require authentication.

- Hardened JWT internals:
  signing key is cached once, minimum secret length is enforced at startup, and token parsing is reused where possible.

## 3. End-to-End Flow
### Register
1. Client calls `/api/auth/register` with email/password/displayName.
2. Password is BCrypt-hashed.
3. User is created with role `USER`.
4. Access + refresh tokens are returned.

### Login
1. Client calls `/api/auth/login` with email/password.
2. `AuthenticationManager` validates credentials through `UserDetailsService` + password encoder.
3. On success, `last_login_at` is updated.
4. Access + refresh tokens are returned.

### Authenticated Request
1. Client sends `Authorization: Bearer <access_token>`.
2. `JwtAuthenticationFilter` parses claims and validates token purpose/expiry.
3. User is loaded from DB and mapped to authorities (for example `ROLE_ADMIN`).
4. Security context is populated.
5. Endpoint authorization rules are applied.

### Refresh
1. Client calls `/api/auth/refresh` with refresh token.
2. Service validates token signature/type/expiry and user binding.
3. New access + refresh tokens are issued (rotation by re-issuance).

## 4. Role Mapping Details
Database role values are `USER` and `ADMIN`.
Spring Security checks use `hasRole("ADMIN")`, which expects authorities prefixed with `ROLE_`.

Because of that, role mapping converts:
- `USER` -> `ROLE_USER`
- `ADMIN` -> `ROLE_ADMIN`

## 5. Key Configuration Decisions
### Stateless + CSRF disabled
CSRF protection is mostly relevant for browser-cookie session flows.
This API is token-based and stateless, so CSRF is disabled in security chain.

### Actuator exposure
Public:
- `/actuator/health/**`
- `/actuator/info`
- `/actuator/prometheus`

Protected (ADMIN):
- remaining `/actuator/**`

Reason: Prometheus scraping is easier when `/actuator/prometheus` is accessible, but sensitive actuator endpoints remain protected.

## 6. JWT Secret Requirement
`JwtTokenProvider` enforces HS256 key strength at startup:
- minimum 32 bytes

Recommended:
- use a random secret with 48+ bytes of entropy

Example generation:
```bash
openssl rand -base64 48
```

Then set:
```bash
export JWT_SECRET='generated-value'
```

## 7. Error/Leakage Considerations
- Invalid auth paths return generic credential errors.
- Global exception handling now avoids exposing internal 500 error details to clients.
- Server logs keep technical details for debugging.

## 8. OAuth2 Note
OAuth2 is intentionally left as extension point, not fully implemented in this phase.
The existing structure already keeps room for it (`OAuthService` + identity model).

## 9. Current TODO
Refresh token revocation persistence is not yet implemented.
Today, refresh is stateless JWT-based. If you need forced logout/device revocation, add server-side refresh token state (for example hashed token id/jti table).

## 10. About the AuthenticationProvider Startup Warning
You may see this Spring Security warning at startup:

"Global AuthenticationManager configured with an AuthenticationProvider bean. UserDetailsService beans will not be used by Spring Security for automatically configuring username/password login..."

In this project, that warning is expected.

Why:
- We intentionally register a custom `AuthenticationProvider` bean (`DaoAuthenticationProvider`) in `SecurityConfig`.
- This is the provider used by the login flow and wired with our `UserDetailsServiceImpl` and password encoder.

Impact:
- It does not break authentication.
- It only means Spring skips its own automatic fallback wiring because explicit provider wiring already exists.

If you want to hide the warning noise, set this logger to ERROR:

```yaml
logging:
  level:
    org.springframework.security.config.annotation.authentication.configuration.InitializeUserDetailsBeanManagerConfigurer: ERROR
```

## 11. About `spring.jpa.open-in-view`
`spring.jpa.open-in-view` is now set to `false` in `application.yaml`.

Reason:
- Prevents lazy-loading database calls from leaking into the web rendering/request completion phase.
- Makes transaction boundaries explicit in service layer code.

What to watch for:
- If a controller accesses a lazy relation outside a service transaction, you can get `LazyInitializationException`.
- Fix by fetching needed associations in service/repository layer (for example fetch join/entity graph/projection) before returning to controller.