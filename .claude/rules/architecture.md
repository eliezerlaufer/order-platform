# Architecture Rules

## Outbox Pattern (MANDATORY for all Kafka publishing)

Never publish to Kafka directly. Always write to `outbox_events` table inside the same transaction.
A scheduler (`@Scheduled`) reads unprocessed rows and publishes them.

```java
// WRONG
kafkaTemplate.send("orders.order.created", payload);

// CORRECT
outboxEventRepository.save(OutboxEvent.of("orders.order.created", payload));
// scheduler handles the actual publish
```

## Kafka Idempotency (MANDATORY for all consumers)

Every `@KafkaListener` must check `ProcessedEvent` before processing:

```java
UUID eventId = UUID.fromString(payload.get("eventId").asText());
if (processedEventRepository.existsById(eventId)) {
    ack.acknowledge();
    return;
}
processedEventRepository.save(ProcessedEvent.of(eventId));
// now process the event
```

## Flyway Rules

- Never add `baseline-on-migrate: true` — with default `baseline-version=1` it skips V1 entirely on fresh DBs
- Migration files: `V1__create_schema.sql`, `V2__add_column.sql` — sequential, no gaps
- Never modify an existing migration — always add a new version

## Kafka Topics

Format: `<domain>.<entity>.<event>`

| Topic                        | Publisher         | Consumers                              |
|------------------------------|-------------------|----------------------------------------|
| orders.order.created         | order-service     | inventory-service, notification        |
| orders.order.confirmed       | order-service     | notification-service                   |
| orders.order.cancelled       | order-service     | inventory-service, notification        |
| inventory.reserved           | inventory-service | payment-service, notification          |
| inventory.reservation.failed | inventory-service | order-service, notification            |
| payments.payment.processed   | payment-service   | order-service, notification            |
| payments.payment.failed      | payment-service   | order-service, inventory, notification |

## Saga Choreography

No orchestrator. Each service reacts to events and publishes its own.
Compensation flows: `inventory.reservation.failed` → order-service cancels order.

## Common Gotchas

- `api-gateway` is on port **8888** — Keycloak occupies 8080
- `notification-service` has no Kafka producer — pure consumer, no outbox table
- `ProductResponse` has no `unitPrice` — frontend must ask user for price
- Frontend `VITE_*` vars are build-time — rebuilding required after changes
