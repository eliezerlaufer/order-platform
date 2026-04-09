# ADR-001 — Saga por Choreography

**Status:** Aceite
**Data:** 2026-04-09

## Contexto

Num sistema de microserviços, uma operação de negócio (criar um pedido) envolve
múltiplos serviços. Precisamos de coordenar estas operações garantindo consistência
mesmo quando algum serviço falha.

Existem duas abordagens para implementar o padrão Saga:

## Opções Consideradas

### Orchestration (Orquestração)
Um serviço central (Saga Orchestrator) diz a cada serviço o que fazer e quando.

```
Orchestrator → order-service: "cria pedido"
Orchestrator → inventory-service: "reserva stock"
Orchestrator → payment-service: "processa pagamento"
```

**Vantagens:** Fluxo visível num só lugar, fácil de debugar.
**Desvantagens:** Acoplamento ao orquestrador, ponto único de falha, mais complexo.

### Choreography (Coreografia) ← ESCOLHIDA
Cada serviço reage a eventos e publica os seus próprios eventos. Não há coordenador central.

```
order-service publica → "orders.order.created"
inventory-service ouve → reserva stock → publica "inventory.reserved"
payment-service ouve → processa → publica "payments.payment.processed"
order-service ouve → confirma pedido
```

**Vantagens:** Baixo acoplamento, cada serviço é autónomo, mais resiliente.
**Desvantagens:** Fluxo distribuído (mais difícil de visualizar sem ferramentas).

## Decisão

**Choreography** — porque este projeto tem poucos serviços e queremos demonstrar
desacoplamento real. Em sistemas com muitas compensações complexas, Orchestration
seria preferível.

## Consequências

- Cada serviço deve ser idempotente (processar o mesmo evento duas vezes tem o mesmo resultado)
- Precisamos de observabilidade (traces distribuídos com OpenTelemetry) para seguir o fluxo
- Usamos Dead Letter Topics (`.DLT`) para mensagens que falham repetidamente
