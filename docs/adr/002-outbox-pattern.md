# ADR-002 — Transactional Outbox Pattern

**Status:** Aceite
**Data:** 2026-04-09

## Contexto

Quando o order-service recebe um pedido, precisa de:
1. Guardar o `Order` na base de dados
2. Publicar o evento `orders.order.created` no Kafka

O problema: estas são duas operações em sistemas distintos (PostgreSQL e Kafka).
Se guardarmos na DB e depois o Kafka falhar, o pedido existe mas o evento nunca é publicado.
Se publicarmos no Kafka primeiro e a DB falhar, temos um evento sem pedido.

## Problema: Dual Write

```
// ERRADO — não há garantia de atomicidade
orderRepository.save(order);      // ← pode ter sucesso
kafkaTemplate.send("orders...");  // ← pode falhar → inconsistência!
```

## Solução: Outbox Pattern

Guardamos o evento na mesma transação de DB que guarda o pedido:

```sql
BEGIN TRANSACTION;
  INSERT INTO orders (...) VALUES (...);
  INSERT INTO outbox_events (event_type, payload) VALUES ('OrderCreated', '{...}');
COMMIT;  -- ambos ou nenhum
```

Um processo separado (`OutboxPublisher`) lê os eventos não publicados e envia para Kafka:

```
OutboxPublisher (scheduled, cada 1s):
  SELECT * FROM outbox_events WHERE published = false
  → kafkaTemplate.send(...)
  → UPDATE outbox_events SET published = true
```

## Decisão

**Usar Transactional Outbox** em todos os serviços que publicam eventos.

## Consequências

- Garantia de "at-least-once delivery" (o evento é publicado pelo menos uma vez)
- Os consumidores devem ser idempotentes (podem receber o mesmo evento duas vezes)
- Latência ligeiramente maior (o publisher corre em polling, não em tempo real)
- Em produção, o Debezium (CDC) é a alternativa mais robusta ao polling manual
