# Order Platform

A microservices-based order management platform built with Spring Boot, React, Kafka, and more. This project implements a distributed system for handling orders, inventory, payments, and notifications using event-driven architecture with the Saga pattern.

## Features

- **Order Management**: Create, confirm, and cancel orders with saga orchestration.
- **Inventory Management**: Real-time stock reservation and updates.
- **Payment Processing**: Secure payment handling with idempotency.
- **Notifications**: Event-driven notifications for order status changes.
- **API Gateway**: Centralized entry point for all services.
- **Frontend**: React 18 + TypeScript UI with Ant Design.
- **Authentication**: Keycloak integration for user management.
- **Observability**: OpenTelemetry traces, Prometheus metrics, and Grafana dashboards (planned for Phase 5).
- **Deployment**: Kubernetes + Helm charts (planned for Phase 6).

## Architecture

The platform follows microservices architecture with the following key patterns:

- **Saga Choreography**: Distributed transactions across services.
- **Outbox Pattern**: Reliable event publishing via database tables.
- **Idempotency**: Duplicate event handling prevention.
- **Event-Driven Communication**: Kafka for asynchronous messaging.

### Services Overview

| Service              | Port | Description |
|----------------------|------|-------------|
| api-gateway          | 8888 | Single entry point for all API requests |
| order-service        | 8081 | Handles order creation, confirmation, and cancellation |
| inventory-service    | 8082 | Manages product inventory and stock reservations |
| payment-service      | 8083 | Processes payments with idempotency |
| notification-service | 8084 | Sends notifications based on events |
| frontend             | 3000 | React application (dev mode) |
| Keycloak             | 8080 | Identity and access management |
| Kafka                | 9092 | Event streaming platform |
| Kafka UI             | 8090 | Kafka management UI |
| PostgreSQL           | 5432 | Database with multiple schemas |

### Kafka Topics

| Topic                        | Publisher          | Consumers |
|------------------------------|--------------------|-----------|
| orders.order.created         | order-service      | inventory-service, notification-service |
| orders.order.confirmed       | order-service      | notification-service |
| orders.order.cancelled       | order-service      | inventory-service, notification-service |
| inventory.reserved           | inventory-service  | payment-service, notification-service |
| inventory.reservation.failed | inventory-service  | order-service, notification-service |
| payments.payment.processed   | payment-service    | order-service, notification-service |
| payments.payment.failed      | payment-service    | order-service, inventory-service, notification-service |

## Prerequisites

- Java 17 or higher
- Node.js 18+ and npm
- Docker and Docker Compose
- Gradle (or use included wrapper)

## Getting Started

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd order-platform
   ```

2. **Start infrastructure services:**
   ```bash
   cd infra
   docker-compose up -d
   ```
   This starts PostgreSQL, Kafka, Keycloak, and other infrastructure.

3. **Build all services:**
   ```bash
   ./gradlew build
   ```

4. **Run services:**
   - Order Service: `./gradlew :order-service:bootRun`
   - Inventory Service: `./gradlew :inventory-service:bootRun`
   - Payment Service: `./gradlew :payment-service:bootRun`
   - Notification Service: `./gradlew :notification-service:bootRun`
   - API Gateway: `./gradlew :api-gateway:bootRun`

5. **Run frontend:**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   The frontend will be available at http://localhost:3000 and proxies API calls to http://localhost:8888.

## Testing

- **Backend Services**: Use Testcontainers for integration tests with real PostgreSQL and Kafka.
  ```bash
  ./gradlew test
  ```
- **Frontend**: Vitest with MSW for mocking.
  ```bash
  cd frontend
  npm run test
  ```

Minimum test coverage: 80% on business logic.

## Development Workflow

Follow GitHub Flow:

1. Create a feature branch: `git checkout -b feature/<service>-<description>`
2. Commit with conventional format: `feat(service): description`
3. Push and open PR against `main`

Branch naming:
- `feature/<service>-<description>` for new features
- `fix/<service>-<description>` for bug fixes
- `ci/<description>` for CI/CD changes
- `docs/<description>` for documentation

## Project Roadmap

- [x] Phase 1: order-service (saga, outbox, Testcontainers)
- [x] Phase 2: inventory-service (Kafka listener, stock reservation)
- [x] Phase 3: payment-service + notification-service
- [x] Phase 4: React 18 + TypeScript + Vite frontend (Ant Design, Keycloak, TanStack Query)
- [ ] Phase 5: Observability (OpenTelemetry traces + Prometheus metrics + Grafana dashboards)
- [ ] Phase 6: Kubernetes + Strimzi (Kafka on K8s) + Helm charts

## Contributing

Contributions are welcome! Please follow the development workflow and ensure all tests pass.
