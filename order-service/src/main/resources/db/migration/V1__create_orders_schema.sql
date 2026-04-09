-- =============================================================================
-- V1 — Schema inicial do order-service
-- =============================================================================
-- Convenção Flyway: V{número}__{descrição}.sql
-- O Flyway aplica estes scripts em ordem crescente e nunca os volta a aplicar.
-- Se precisares de alterar o schema, crias um V2__, nunca editas o V1__.
-- =============================================================================

-- Tipo enum para o estado do pedido
-- O pedido passa pelos estados: PENDING → CONFIRMED → SHIPPED → DELIVERED
-- Ou: PENDING → CANCELLED (se stock ou pagamento falharem)
CREATE TYPE order_status AS ENUM (
    'PENDING',      -- pedido recebido, a aguardar processamento
    'CONFIRMED',    -- stock reservado e pagamento aprovado
    'SHIPPED',      -- em trânsito
    'DELIVERED',    -- entregue ao cliente
    'CANCELLED'     -- cancelado (stock indisponível ou pagamento recusado)
);

-- Tabela principal de pedidos
CREATE TABLE orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id     UUID NOT NULL,                    -- ID do cliente (vem do token JWT)
    status          order_status NOT NULL DEFAULT 'PENDING',
    total_amount    NUMERIC(10, 2) NOT NULL,          -- valor total do pedido
    currency        VARCHAR(3) NOT NULL DEFAULT 'EUR',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0         -- optimistic locking (controlo de concorrência)
);

-- Linha de cada pedido (um pedido pode ter vários produtos)
CREATE TABLE order_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id      UUID NOT NULL,           -- ID do produto (gerido pelo inventory-service)
    product_name    VARCHAR(255) NOT NULL,   -- snapshot do nome (imutável após pedido)
    quantity        INTEGER NOT NULL CHECK (quantity > 0),
    unit_price      NUMERIC(10, 2) NOT NULL CHECK (unit_price >= 0),
    total_price     NUMERIC(10, 2) NOT NULL  -- quantity * unit_price
);

-- =============================================================================
-- OUTBOX TABLE — Padrão Transactional Outbox
-- =============================================================================
-- Em vez de publicar diretamente no Kafka, guardamos o evento aqui na mesma
-- transação que altera o pedido. Um processo separado (outbox publisher) lê
-- esta tabela e publica no Kafka, garantindo que nunca perdemos eventos.
--
-- Porquê? Se publicássemos no Kafka dentro da transação e o Kafka falhasse,
-- o pedido ficaria guardado mas o evento nunca chegaria aos outros serviços.
-- =============================================================================
CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(100) NOT NULL,   -- ex: "Order"
    aggregate_id    UUID NOT NULL,           -- ID do pedido
    event_type      VARCHAR(100) NOT NULL,   -- ex: "OrderCreated", "OrderCancelled"
    payload         JSONB NOT NULL,          -- conteúdo do evento em JSON
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMP WITH TIME ZONE,  -- NULL = ainda não publicado no Kafka
    published       BOOLEAN NOT NULL DEFAULT FALSE
);

-- Índices para performance
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_outbox_events_published ON outbox_events(published) WHERE published = FALSE;
