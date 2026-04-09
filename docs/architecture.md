# Arquitetura — Order Processing Platform

## Visão Geral

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

## Serviços

| Serviço | Porta | Responsabilidade |
|---------|-------|-----------------|
| api-gateway | 8888 | Ponto de entrada único, routing, auth |
| order-service | 8081 | Ciclo de vida dos pedidos |
| inventory-service | 8082 | Stock e reservas de produtos |
| payment-service | 8083 | Processamento de pagamentos |
| notification-service | 8084 | Emails e notificações |
| frontend | 3000 | SPA React |

## Infraestrutura Local (Docker Compose)

| Serviço | Porta | Acesso |
|---------|-------|--------|
| PostgreSQL | 5432 | `jdbc:postgresql://localhost:5432/{db}` |
| Kafka | 9092 | `localhost:9092` |
| Kafka UI | 8090 | http://localhost:8090 |
| Keycloak | 8080 | http://localhost:8080 (admin/admin) |

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

## Decisões de Arquitetura

Ver pasta `docs/adr/` para Architecture Decision Records detalhados.

- [ADR-001](adr/001-saga-choreography.md) — Saga por Choreography vs Orchestration
- [ADR-002](adr/002-outbox-pattern.md) — Transactional Outbox Pattern
- [ADR-003](adr/003-database-per-service.md) — Database per Service
