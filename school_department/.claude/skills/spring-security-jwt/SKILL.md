---
name: spring-security-jwt
description: Pitfalls and checklist for Spring Security 7 + jjwt 0.12 stateful JWT in this project (filter chain, filter error handling via HandlerExceptionResolver, catch order, ROLE_ prefix, blacklist, refresh rotation rollback trap, locale in filter errors, Swagger bearer). Load before writing or reviewing any security, token, login or authorization code.
---

# Spring Security + JWT pitfalls

These are the places beginners get wrong. Check every one when you write or review security code.

## Filter chain

- `SessionCreationPolicy.STATELESS`, CSRF / formLogin / httpBasic disabled.
- JWT filter is added with `addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)`.
- The JWT filter is a `@Component`, so Boot also registers it as a plain servlet filter. Disable that with a `FilterRegistrationBean` that has `setEnabled(false)`, or the filter runs twice.
- URL rules: most specific first, `anyRequest().authenticated()` last. `requestMatchers(HttpMethod.GET, "/api/students/**")` does not match the context path (`/test-demo`), matchers are relative to it.
- `hasRole("ADMIN")` checks authority `ROLE_ADMIN`. Create authorities as `"ROLE_" + role.name()`.

## Errors from the security layer

- `@RestControllerAdvice` does not see exceptions thrown in a filter. Pass them to `@Qualifier("handlerExceptionResolver") HandlerExceptionResolver` with `resolver.resolveException(request, response, null, ex)` so `GlobalExceptionHandler` writes the usual `ErrorResponse`.
- `AuthenticationEntryPoint` (401) and `AccessDeniedHandler` (403) do the same: delegate to the resolver, do not write JSON by hand.
- `GlobalExceptionHandler` needs handlers for `BadCredentialsException`, `DisabledException`, and `AccessDeniedException` (this also catches `AuthorizationDeniedException` from `@PreAuthorize`). Without the last one `@PreAuthorize` failures become 500.
- In filter errors the DispatcherServlet has not run yet, so `LocaleContextHolder` is not set by the `LocaleResolver`. Resolve the locale from the request with the `LocaleResolver` bean inside `build(...)`.
- In jjwt `ExpiredJwtException` extends `JwtException`. Catch it first.

## Tokens

- Key is built once in the constructor: `Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))`. A short secret throws `WeakKeyException`.
- Every token gets a fresh `jti` (UUID). Store only the `jti` in the DB, never the token string.
- Role in the SecurityContext comes from the DB account, not from the token claim.
- Refresh token sent as Bearer must be rejected (type claim check).
- Never put password or email in claims. Never log a token or password.

## Stateful blacklist and rotation

- Blacklist check on every protected request: `existsByJti(jti)` on a unique-indexed column.
- `blacklist(...)` is idempotent and skips already-expired tokens. A parallel duplicate insert throws `DataIntegrityViolationException` on the unique key: catch it, log at DEBUG, do not turn it into 409.
- Reuse detection: revoke everything then throw. If the method is `@Transactional`, the `RuntimeException` rolls the revoke back. Use `@Transactional(noRollbackFor = UnauthorizedException.class)` (or `REQUIRES_NEW` for the revoke) and explain the choice in a short comment.
- `@Scheduled` cleanup job runs outside HTTP: put a `traceId` into MDC at start, remove it in `finally`. `@EnableScheduling` must be on.

## Logging

- MDC key `username` set by the JWT filter, removed in `finally`. Log pattern `%X{username:-anonymous}`.
- `LoggingAspect` prints controller args: mask `password`, `oldPassword`, `newPassword`, `refreshToken`, `accessToken`. Request records holding secrets override `toString()`.

## Swagger

- `bearerAuth` HTTP bearer JWT scheme + global `SecurityRequirement`. Public endpoints get an empty `@SecurityRequirements()`.
- The existing `OpenApiCustomizer` adds 401/403 `ErrorResponse` to protected operations.
