# Testing Rules

## Backend — Integration Tests

All integration tests MUST use Testcontainers. Never mock PostgreSQL or Kafka.

```java
// Every integration test class must extend BaseIntegrationTest
class PaymentServiceIntegrationTest extends BaseIntegrationTest {
    // BaseIntegrationTest spins up real PostgreSQL + Kafka containers
}

// WRONG — tries to connect to localhost:5432 which doesn't exist in CI
@SpringBootTest
class PaymentServiceApplicationTests { }

// CORRECT
class PaymentServiceApplicationTests extends BaseIntegrationTest { }
```

`BaseIntegrationTest` exists in each service's test package. It:
- Starts a `postgres:16-alpine` container
- Starts a `confluentinc/cp-kafka:7.6.0` container
- Overrides `spring.datasource.*` and `spring.kafka.bootstrap-servers` via `@DynamicPropertySource`
- Sets `spring.security.oauth2.resourceserver.jwt.issuer-uri` to a dummy URL (no Keycloak needed)

## Backend — Unit Tests

Use `@ExtendWith(MockitoExtension.class)` for pure unit tests that don't need Spring context.
Never use `@SpringBootTest` for unit tests.

## Frontend — Vitest + MSW

```typescript
// All API calls must be intercepted by MSW — no real HTTP in tests
import { server } from '@/mocks/server'

// server.listen() / resetHandlers() / close() are in src/test-setup.ts — already configured

// Override a handler in a specific test:
server.use(
  http.get('/api/orders', () => HttpResponse.json([], { status: 200 }))
)
```

MSW handlers are in `frontend/src/mocks/handlers.ts`.
Mock data fixtures are exported from `handlers.ts` — reuse them, don't create new ones.

## Coverage Requirements

- Backend business logic (services, domain): 80%+
- Frontend hooks and utilities: 80%+
- Controllers and event listeners: covered by integration tests

## CI Behaviour

- `TESTCONTAINERS_RYUK_DISABLED=true` is set in CI — do not add it locally
- Tests run in parallel: `./gradlew test --parallel`
- Frontend tests run in CI mode: `npm run test:run` (no watch, exits after run)
