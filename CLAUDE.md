# Order Platform — Claude Instructions

## Git Workflow (GitHub Flow)

**Before starting any new task:**
```bash
git checkout main
git pull origin main
git checkout -b feature/<service>-<description>
# Examples:
#   feature/order-service-cancel-endpoint
#   feature/frontend-order-filters
#   feature/infra-prometheus
#   fix/payment-service-idempotency
```

**After finishing:**
1. Commit with conventional commits format: `feat(service): description`
2. Push branch and open PR against `main`
3. Never push directly to `main` — branch protection is enforced

**Branch naming:**
- `feature/<service>-<description>` — new functionality
- `fix/<service>-<description>` — bug fixes
- `ci/<description>` — CI/CD changes
- `docs/<description>` — documentation only

## Project Roadmap

- [x] FASE 1 — order-service (saga, outbox, Testcontainers)
- [x] FASE 2 — inventory-service (Kafka listener, stock reservation)
- [x] FASE 3 — payment-service + notification-service
- [x] FASE 4 — React 18 + TypeScript + Vite frontend (Ant Design, Keycloak, TanStack Query)
- [ ] FASE 5 — Observability (OpenTelemetry traces + Prometheus metrics + Grafana dashboards)
- [ ] FASE 6 — Kubernetes + Strimzi (Kafka on K8s) + Helm charts

## Services & Ports

| Service              | Port | Notes                        |
|----------------------|------|------------------------------|
| api-gateway          | 8888 | Single entry point (NOT 8080)|
| order-service        | 8081 |                              |
| inventory-service    | 8082 | products at /api/products/** |
| payment-service      | 8083 |                              |
| notification-service | 8084 |                              |
| frontend (dev)       | 3000 | Vite proxy → 8888            |
| Keycloak             | 8080 | realm: order-platform        |
| Kafka                | 9092 | KRaft, no Zookeeper          |
| Kafka UI             | 8090 |                              |
| PostgreSQL           | 5432 | 4 databases                  |

## Architecture Patterns (enforce these)

- **Outbox pattern** — never publish Kafka events directly; always write to `outbox_events` table first
- **Idempotency** — every Kafka listener must check `ProcessedEvent` table before processing
- **Flyway** — never use `baseline-on-migrate: true` (skips V1 migration on fresh DBs)
- **DTOs as records** — immutable Java records for all request/response DTOs
- **Manual Kafka ack** — `ack-mode: MANUAL_IMMEDIATE`, acknowledge only after transaction commits

## Testing Requirements

- All services: Testcontainers (real PostgreSQL + Kafka), no mocks for DB/Kafka
- Integration tests extend `BaseIntegrationTest` — never use bare `@SpringBootTest` without Testcontainers
- Frontend: Vitest + MSW (mock service worker), no real HTTP calls in tests
- Minimum 80% coverage on business logic

## Kafka Topics

Format: `<domain>.<entity>.<event>`

| Topic                        | Publisher          | Consumers                          |
|------------------------------|--------------------|------------------------------------|
| orders.order.created         | order-service      | inventory-service, notification    |
| orders.order.confirmed       | order-service      | notification-service               |
| orders.order.cancelled       | order-service      | inventory-service, notification    |
| inventory.reserved           | inventory-service  | payment-service, notification      |
| inventory.reservation.failed | inventory-service  | order-service, notification        |
| payments.payment.processed   | payment-service    | order-service, notification        |
| payments.payment.failed      | payment-service    | order-service, inventory, notification |

## Common Gotchas

- `api-gateway` runs on **8888**, not 8080 — Keycloak uses 8080
- `notification-service` has no Kafka producer — pure consumer, no outbox
- `ProductResponse` has no `unitPrice` field — frontend must ask user for price on order creation
- Frontend `VITE_*` env vars are **build-time only** — changes require rebuild, not restart
