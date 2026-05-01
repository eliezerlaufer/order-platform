# Order Processing Platform

Plataforma de processamento de pedidos baseada em microserviços com **saga choreography**, **transactional outbox pattern** e comunicação assíncrona via **Apache Kafka**.

## Arquitetura

```
                        ┌──────────────────┐
                        │   Frontend React  │  :3000
                        │   (Vite + TS)     │
                        └────────┬─────────┘
                                 │ HTTP/REST
                        ┌────────▼─────────┐
                        │   API Gateway     │  :8888
                        │  (Spring Cloud)   │
                        └─┬──────┬────┬────┘
                          │      │    │
              ┌───────────▼┐  ┌──▼──┐ ┌▼────────────────┐
              │order-service│  │inv  │ │payment-service  │
              │    :8081    │  │:8082│ │    :8083        │
              └──────┬──────┘  └──┬──┘ └────────┬────────┘
                     │            │              │
              ┌──────▼────────────▼──────────────▼────────┐
              │              Apache Kafka :9092            │
              │  (event bus — todos os serviços comunicam) │
              └──────────────────┬─────────────────────────┘
                                 │
                        ┌────────▼─────────┐
                        │notification-serv │  :8084
                        │    (email/SMS)    │
                        └──────────────────┘

Cada serviço tem a sua própria base de dados PostgreSQL:
  orders_db / inventory_db / payments_db / notifications_db
```

### Stack

| Camada | Tecnologia |
|--------|-----------|
| Backend | Java 21, Spring Boot 3.5, Gradle |
| Messaging | Apache Kafka (KRaft, sem Zookeeper) |
| Database | PostgreSQL 16 (database-per-service) |
| Frontend | React 18, TypeScript, Vite, Ant Design |
| Auth | Keycloak (OAuth2/JWT) |
| Observability | OpenTelemetry, Prometheus, Grafana, Jaeger |
| Containers | Docker Compose (local), Kubernetes + Strimzi (prod) |

### Padrões

- **Saga Choreography** — fluxo de pedidos via eventos Kafka, sem orquestrador central
- **Transactional Outbox** — eventos escritos na tabela `outbox_events` na mesma transação; `OutboxPublisher` faz polling a cada 5s e publica no Kafka
- **Idempotência** — cada consumer verifica a tabela `processed_events` antes de processar (proteção contra duplicados at-least-once)
- **Database per Service** — cada serviço tem a sua própria base de dados PostgreSQL
- **CQRS light** — modelos de leitura via projeções onde necessário

## Serviços

| Serviço | Porta | Responsabilidade |
|---------|-------|-----------------|
| api-gateway | 8888 | Ponto de entrada único, routing, auth, rate limiting |
| order-service | 8081 | Ciclo de vida dos pedidos |
| inventory-service | 8082 | Stock e reservas de produtos |
| payment-service | 8083 | Processamento de pagamentos |
| notification-service | 8084 | Emails e notificações |
| frontend | 3000 | SPA React |

## Fluxo de um Pedido (Saga Choreography)

```
1. POST /api/orders → order-service
2. order-service guarda Order(PENDING) + OutboxEvent → orders_db [transação única]
3. OutboxPublisher lê outbox → publica "orders.order.created" no Kafka
4. inventory-service consome → verifica stock
   ├── Stock OK  → publica "inventory.reserved"
   └── Sem stock → publica "inventory.reservation.failed"
5. payment-service consome "inventory.reserved" → processa pagamento
   ├── OK  → publica "payments.payment.processed"
   └── NOK → publica "payments.payment.failed"
6. order-service consome resultado:
   ├── "payments.payment.processed" → Order(CONFIRMED)
   └── "payments.payment.failed"   → Order(CANCELLED) + compensa inventário
7. notification-service consome → envia email ao cliente
```

## Tópicos Kafka

| Tópico | Produtor | Consumidor |
|--------|----------|-----------|
| `orders.order.created` | order-service | inventory-service |
| `orders.order.cancelled` | order-service | inventory-service, notification-service |
| `inventory.reserved` | inventory-service | payment-service |
| `inventory.reservation.failed` | inventory-service | order-service |
| `payments.payment.processed` | payment-service | order-service, notification-service |
| `payments.payment.failed` | payment-service | order-service, notification-service |

## Quick Start

### 1. Infraestrutura (Docker Compose)

```bash
cd infra
docker compose up -d
```

| Serviço | Porta | Acesso |
|---------|-------|--------|
| PostgreSQL | 5432 | `jdbc:postgresql://localhost:5432/{db}` |
| Kafka | 9092 | Protocolo binário (não HTTP) |
| Kafka UI | 8090 | http://localhost:8090 |
| Keycloak | 8080 | http://localhost:8080 (admin/admin) |
| Jaeger UI | 16686 | http://localhost:16686 |
| Prometheus | 9090 | http://localhost:9090 |
| Grafana | 3001 | http://localhost:3001 (admin/admin) |

### 2. Configurar Keycloak

Criar o realm, client e user de teste:

```bash
# Obter token admin
TOKEN=$(curl -s -X POST "http://localhost:8080/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=admin&grant_type=password&client_id=admin-cli" \
  | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

# Criar realm
curl -s -X POST "http://localhost:8080/admin/realms" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"realm":"order-platform","enabled":true}'

# Criar client (frontend SPA)
curl -s -X POST "http://localhost:8080/admin/realms/order-platform/clients" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"clientId":"frontend","enabled":true,"publicClient":true,"directAccessGrantsEnabled":true,"redirectUris":["http://localhost:3000/*"],"webOrigins":["http://localhost:3000"],"standardFlowEnabled":true}'

# Criar user de teste (testuser / test123)
curl -s -X POST "http://localhost:8080/admin/realms/order-platform/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","enabled":true,"emailVerified":true,"firstName":"Test","lastName":"User","email":"test@order-platform.local","credentials":[{"type":"password","value":"test123","temporary":false}]}'
```

### 3. Backend (Gradle)

```bash
# Build
./gradlew build

# Iniciar todos os serviços (cada um num terminal)
./gradlew :order-service:bootRun
./gradlew :inventory-service:bootRun
./gradlew :payment-service:bootRun
./gradlew :notification-service:bootRun
./gradlew :api-gateway:bootRun
```

Ou configurar um **Compound Run Configuration** no IntelliJ com 5 configs Gradle (`bootRun` para cada serviço) para iniciar todos com um clique.

### 4. Frontend

O frontend corre via Docker Compose (porta 3000) com nginx proxy para o api-gateway.
Login com `testuser` / `test123`.

Para desenvolvimento local:

```bash
cd frontend
npm install
npm run dev
```

## Observability

- **Traces**: OpenTelemetry → OTel Collector → Jaeger (http://localhost:16686)
- **Metrics**: Micrometer → Prometheus (http://localhost:9090) → Grafana (http://localhost:3001)
- **Health**: Spring Actuator `/actuator/health` em cada serviço

## Testing

```bash
# Backend — Testcontainers (PostgreSQL + Kafka reais)
./gradlew test

# Frontend — Vitest + MSW
cd frontend
npm run test
```

Cobertura mínima: 80% na lógica de negócio.

## Development Workflow

GitHub Flow:

1. Criar branch: `git checkout -b feature/<service>-<description>`
2. Commit convencional: `feat(service): description`
3. Push e abrir PR contra `main`

## Decisões de Arquitetura

Ver pasta `docs/adr/` para Architecture Decision Records:

- [ADR-001](docs/adr/001-saga-choreography.md) — Saga por Choreography vs Orchestration
- [ADR-002](docs/adr/002-outbox-pattern.md) — Transactional Outbox Pattern
- [ADR-003](docs/adr/003-database-per-service.md) — Database per Service
