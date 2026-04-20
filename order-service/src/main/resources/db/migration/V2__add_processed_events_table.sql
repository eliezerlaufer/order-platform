-- =============================================================================
-- V2: Tabela de idempotência para consumidor Kafka
-- =============================================================================
-- Antes de processar um evento, inserimos o eventId aqui. Se já existir,
-- o evento é duplicado e deve ser ignorado.
-- =============================================================================

CREATE TABLE processed_events (
    event_id    UUID        PRIMARY KEY,
    consumed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
