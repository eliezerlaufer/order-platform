# Security Rules

## Authentication

- All endpoints are protected by JWT (Keycloak) via Spring Security
- `customerId` is ALWAYS extracted from the JWT token — never from the request body
- Never trust client-provided user identity

```java
// CORRECT — extract from JWT
@GetMapping
public ResponseEntity<?> getOrders(JwtAuthenticationToken token) {
    UUID customerId = UUID.fromString(token.getName());
}

// WRONG — trust the client
@GetMapping
public ResponseEntity<?> getOrders(@RequestParam UUID customerId) { }
```

## Secrets — never hardcode

```yaml
# WRONG in application.yml
spring.datasource.password: mypassword123

# CORRECT — read from environment
spring.datasource.password: ${DB_PASSWORD}
```

In tests, Testcontainers provides real credentials dynamically via `@DynamicPropertySource`.

## Input Validation

- Always use `@Valid` on `@RequestBody` parameters
- Bean Validation annotations on all DTO fields (`@NotNull`, `@NotBlank`, `@Min`, etc.)
- Validation errors return 400 — handled by `GlobalExceptionHandler`

```java
// CORRECT
public ResponseEntity<?> create(@Valid @RequestBody CreateOrderRequest request) { }
```

## SQL — no raw queries

- All DB access via Spring Data JPA repositories
- If raw SQL is necessary, use `@Query` with named parameters — never string concatenation

```java
// WRONG
entityManager.createQuery("SELECT o FROM Order o WHERE id = '" + id + "'")

// CORRECT
@Query("SELECT o FROM Order o WHERE o.id = :id")
Optional<Order> findById(@Param("id") UUID id);
```

## Frontend Security

- JWT token is stored in memory only (Keycloak JS) — never in localStorage
- All API calls go through the api-gateway (port 8888) — never directly to services
- `VITE_*` env vars are public — never put secrets in them
- XSS: Ant Design escapes content by default — never use `dangerouslySetInnerHTML`

## Pre-commit Checklist

Before committing:
- [ ] No hardcoded passwords, API keys, or tokens
- [ ] All new endpoints have `@Valid` on request bodies
- [ ] `customerId` extracted from JWT, not from request
- [ ] No `System.out.println` or raw stack trace logging
