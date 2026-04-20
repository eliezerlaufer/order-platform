# Order Platform — Claude Instructions

## Commands

### Backend (Gradle)
```bash
./gradlew build --no-daemon --parallel          # build todos os serviços
./gradlew test --no-daemon --parallel           # todos os testes (Testcontainers)
./gradlew :order-service:test --no-daemon       # testar um serviço específico
./gradlew compileJava compileTestJava --no-daemon --parallel  # só compilar
./gradlew :order-service:bootJar --no-daemon    # gerar JAR
```

### Frontend
```bash
cd frontend && npm run dev      # dev server (porta 3000)
cd frontend && npm run build    # build produção
cd frontend && npm run lint     # ESLint
cd frontend && npm run test:run # Vitest (CI mode, sem watch)
cd frontend && npm test         # Vitest (watch mode)
```

### Infra local
```bash
cd infra && docker compose up -d        # sobe tudo (Postgres, Kafka, Keycloak)
cd infra && docker compose down         # para tudo
cd infra && docker compose logs -f kafka
```

## Git Workflow (GitHub Flow)

```bash
# Antes de qualquer nova task:
git checkout main && git pull origin main
git checkout -b feature/<service>-<description>
# fix/<service>-<description> | ci/<description> | docs/<description>

# Depois:
git commit -m "feat(order-service): add cancel endpoint"
git push -u origin <branch>
# Abrir PR contra main — nunca push direto para main
```

## Project Roadmap

- [x] FASE 1 — order-service
- [x] FASE 2 — inventory-service
- [x] FASE 3 — payment-service + notification-service
- [x] FASE 4 — React 18 + TypeScript + Vite frontend
- [ ] FASE 5 — Observability (OpenTelemetry + Prometheus + Grafana)
- [ ] FASE 6 — Kubernetes + Strimzi + Helm

## Services & Ports

| Service              | Port | Notes                         |
|----------------------|------|-------------------------------|
| api-gateway          | 8888 | Entry point — NOT 8080        |
| order-service        | 8081 |                               |
| inventory-service    | 8082 | products at /api/products/**  |
| payment-service      | 8083 |                               |
| notification-service | 8084 | pure consumer, no outbox      |
| frontend (dev)       | 3000 | Vite proxy → 8888             |
| Keycloak             | 8080 | realm: order-platform         |
| Kafka                | 9092 | KRaft, no Zookeeper           |
| PostgreSQL           | 5432 | 4 databases                   |

## Rules

See `.claude/rules/` for detailed guidelines:
- [architecture.md](.claude/rules/architecture.md) — Outbox, Saga, idempotency patterns
- [testing.md](.claude/rules/testing.md) — Testcontainers, Vitest, MSW
- [code-style.md](.claude/rules/code-style.md) — Java records, TypeScript strict mode
- [security.md](.claude/rules/security.md) — auth, secrets, input validation
