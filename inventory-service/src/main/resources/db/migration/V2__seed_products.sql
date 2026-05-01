-- =============================================================================
-- V2 — Dados iniciais de produtos para desenvolvimento
-- =============================================================================

INSERT INTO products (id, name, sku, available_quantity) VALUES
    (gen_random_uuid(), 'Laptop Dell XPS 15',       'LAPTOP-DELL-XPS15',   50),
    (gen_random_uuid(), 'Monitor LG 27" 4K',        'MONITOR-LG-27-4K',    30),
    (gen_random_uuid(), 'Teclado Mecânico Keychron', 'KB-KEYCHRON-K2',     100),
    (gen_random_uuid(), 'Mouse Logitech MX Master',  'MOUSE-LOG-MXM3',      80),
    (gen_random_uuid(), 'Webcam Logitech C920',      'CAM-LOG-C920',        60),
    (gen_random_uuid(), 'Headset Sony WH-1000XM5',   'AUDIO-SONY-XM5',      40),
    (gen_random_uuid(), 'SSD Samsung 1TB NVMe',      'SSD-SAM-1TB',         120),
    (gen_random_uuid(), 'Hub USB-C Anker 7-in-1',    'HUB-ANKER-7IN1',      90),
    (gen_random_uuid(), 'Cabo HDMI 2.1 2m',          'CABLE-HDMI21-2M',    200),
    (gen_random_uuid(), 'Suporte Monitor Ergonômico', 'STAND-ERGO-01',       45);
