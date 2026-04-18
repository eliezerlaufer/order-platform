-- =============================================================================
-- V1 — Schema inicial do payment-service
-- =============================================================================
-- Convenção Flyway: V{número}__{descrição}.sql
-- Nunca editar um V já aplicado — criar V2__, V3__, etc.
-- =============================================================================

-- =============================================================================
-- ESTADOS DE UM PAGAMENTO
-- =============================================================================
--   PENDING   → criado, aguarda resposta do gateway
--   PROCESSED → aprovado pelo gateway (mock Stripe)
--   FAILED    → recusado pelo gateway
-- =============================================================================
CREATE TYPE payment_status AS ENUM (
    'PENDING',
    'PROCESSED',
    'FAILED'
);

-- =============================================================================
-- PAGAMENTOS
-- =============================================================================
-- Um pagamento por order (one-to-one). Se o pagamento falhar, a order
-- permanece em PENDING e o stock é libertado via compensação Saga.
-- =============================================================================
CREATE TABLE payments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID NOT NULL UNIQUE,
    amount      NUMERIC(19, 2) NOT NULL,
    status      payment_status NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- OUTBOX — mesmo padrão do order-service e inventory-service
-- =============================================================================
CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    UUID NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMP WITH TIME ZONE,
    published       BOOLEAN NOT NULL DEFAULT FALSE
);

-- =============================================================================
-- PROCESSED EVENTS — idempotência do consumidor Kafka
-- =============================================================================
CREATE TABLE processed_events (
    event_id    UUID PRIMARY KEY,
    consumed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- ÍNDICES
-- =============================================================================
CREATE INDEX idx_payments_order_id   ON payments(order_id);
CREATE INDEX idx_payments_status     ON payments(status);
CREATE INDEX idx_outbox_events_pending ON outbox_events(published) WHERE published = FALSE;
