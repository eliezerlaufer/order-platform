-- =============================================================================
-- V1 — Schema inicial do inventory-service
-- =============================================================================
-- Convenção Flyway: V{número}__{descrição}.sql
-- Nunca editar um V já aplicado — criar V2__, V3__, etc.
-- =============================================================================

-- =============================================================================
-- ESTADOS DE UMA RESERVA
-- =============================================================================
--   RESERVED  → stock retirado do disponível, à espera de confirmação
--   RELEASED  → reserva cancelada (stock devolvido ao disponível)
--   CONFIRMED → pagamento aprovado (stock sai definitivamente)
-- =============================================================================
CREATE TYPE reservation_status AS ENUM (
    'RESERVED',
    'RELEASED',
    'CONFIRMED'
);

-- =============================================================================
-- CATÁLOGO DE PRODUTOS
-- =============================================================================
-- available_quantity → unidades disponíveis para reserva
-- reserved_quantity  → unidades já reservadas (ainda não confirmadas)
-- O total físico em armazém = available + reserved
-- =============================================================================
CREATE TABLE products (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(255) NOT NULL,
    sku                 VARCHAR(100) NOT NULL UNIQUE,
    available_quantity  INTEGER NOT NULL DEFAULT 0 CHECK (available_quantity >= 0),
    reserved_quantity   INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0  -- optimistic locking
);

-- =============================================================================
-- RESERVAS DE STOCK
-- =============================================================================
-- Uma reserva liga uma quantidade de um produto a um pedido.
-- Um pedido pode ter múltiplas reservas (uma por produto).
-- =============================================================================
CREATE TABLE stock_reservations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID NOT NULL,
    product_id  UUID NOT NULL REFERENCES products(id),
    quantity    INTEGER NOT NULL CHECK (quantity > 0),
    status      reservation_status NOT NULL DEFAULT 'RESERVED',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- OUTBOX — mesmo padrão do order-service
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
-- PROCESSED EVENTS — idempotência do consumidor
-- =============================================================================
-- Cada evento consumido do Kafka tem um UUID (eventId no payload).
-- Antes de processar, inserimos aqui; se já existir (duplicate key), ignoramos.
-- Isto protege-nos da entrega at-least-once do Kafka/Outbox.
-- =============================================================================
CREATE TABLE processed_events (
    event_id    UUID PRIMARY KEY,
    consumed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- ÍNDICES
-- =============================================================================
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_stock_reservations_order_id ON stock_reservations(order_id);
CREATE INDEX idx_stock_reservations_product_id ON stock_reservations(product_id);
CREATE INDEX idx_stock_reservations_status ON stock_reservations(status);
CREATE INDEX idx_outbox_events_published ON outbox_events(published) WHERE published = FALSE;
