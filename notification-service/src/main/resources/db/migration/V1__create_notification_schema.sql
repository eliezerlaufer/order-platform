-- =============================================================================
-- V1 — Schema inicial do notification-service
-- =============================================================================

-- =============================================================================
-- TIPOS DE NOTIFICAÇÃO
-- =============================================================================
--   ORDER_CREATED          → pedido criado com sucesso
--   ORDER_CANCELLED        → pedido cancelado
--   PAYMENT_PROCESSED      → pagamento aprovado
--   PAYMENT_FAILED         → pagamento recusado
--   STOCK_UNAVAILABLE      → stock insuficiente para a reserva
-- =============================================================================
CREATE TYPE notification_type AS ENUM (
    'ORDER_CREATED',
    'ORDER_CANCELLED',
    'PAYMENT_PROCESSED',
    'PAYMENT_FAILED',
    'STOCK_UNAVAILABLE'
);

-- =============================================================================
-- ORDER CONTEXT — cache local para resolução de customerId
-- =============================================================================
-- Populado ao consumir orders.order.created (único evento que traz customerId).
-- Outros eventos (payments.*, inventory.*) usam esta tabela para saber a quem
-- enviar a notificação.
-- =============================================================================
CREATE TABLE order_contexts (
    order_id    UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- NOTIFICAÇÕES
-- =============================================================================
-- Registo de cada notificação enviada (email/SMS mock). Em produção, o campo
-- status poderia ser PENDING/SENT/FAILED para suportar re-tentativas.
-- =============================================================================
CREATE TABLE notifications (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID NOT NULL,
    customer_id UUID NOT NULL,
    type        notification_type NOT NULL,
    channel     VARCHAR(50) NOT NULL DEFAULT 'EMAIL',
    message     TEXT NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- PROCESSED EVENTS — idempotência do consumidor
-- =============================================================================
CREATE TABLE processed_events (
    event_id    UUID PRIMARY KEY,
    consumed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- ÍNDICES
-- =============================================================================
CREATE INDEX idx_notifications_order_id    ON notifications(order_id);
CREATE INDEX idx_notifications_customer_id ON notifications(customer_id);
CREATE INDEX idx_notifications_type        ON notifications(type);
