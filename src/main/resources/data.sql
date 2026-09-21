-- =========================================================================
-- DATOS SEMILLA ARKA (H2 DATABASE IN-MEMORY)
-- Compatible con MERGE INTO ... KEY(id) para evitar duplicados en reinicios
-- =========================================================================

-- 1. PRODUCTOS DE CATÁLOGO (Accesorios para PC)
MERGE INTO inventory.products (id, name, description, price, category, attributes) KEY(id) VALUES
('11111111-1111-1111-1111-111111111111', 'Mouse Ergonómico Inalámbrico Pro', 'Sensor óptico de alta precisión 16000 DPI con batería recargable', 120.00, 'Periféricos', '{"marca": "Logitech", "conectividad": "Wireless / Bluetooth"}'),
('22222222-2222-2222-2222-222222222222', 'Teclado Mecánico RGB Switch Blue', 'Teclas mecánicas de respuesta rápida retroiluminadas y chasis de aluminio', 250.00, 'Periféricos', '{"marca": "Redragon", "distribucion": "ISO Español"}'),
('33333333-3333-3333-3333-333333333333', 'Monitor Gamer 27 Pulgadas 165Hz', 'Panel IPS QHD 2K 1ms compatible con G-Sync y FreeSync', 899.00, 'Monitores', '{"marca": "ASUS ROG", "tasa_refresco": "165Hz"}'),
('44444444-4444-4444-4444-444444444444', 'Cable HDMI 2.1 Ultra High Speed', 'Soporta 8K a 60Hz y 4K a 120Hz con conectores chapados en oro', 25.00, 'Cables', '{"longitud": "2 metros"}');

-- 2. INVENTARIO FÍSICO Y UMBRALES
MERGE INTO inventory.inventory_items (id, product_id, physical_stock, reserved_stock, minimum_threshold, updated_at) KEY(id) VALUES
('a1111111-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 50, 0, 10, CURRENT_TIMESTAMP),
('a2222222-0000-0000-0000-000000000002', '22222222-2222-2222-2222-222222222222', 30, 0, 5, CURRENT_TIMESTAMP),
('a3333333-0000-0000-0000-000000000003', '33333333-3333-3333-3333-333333333333', 15, 0, 5, CURRENT_TIMESTAMP),
('a4444444-0000-0000-0000-000000000004', '44444444-4444-4444-4444-444444444444', 3, 0, 10, CURRENT_TIMESTAMP);

-- 3. PROYECCIÓN DE REABASTECIMIENTO INICIAL (Producto 4 bajo umbral crítico: 3 <= 10)
MERGE INTO analytics.projection_replenishment (product_id, current_stock, minimum_threshold, updated_at) KEY(product_id) VALUES
('44444444-4444-4444-4444-444444444444', 3, 10, CURRENT_TIMESTAMP);
