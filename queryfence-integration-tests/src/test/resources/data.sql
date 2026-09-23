DELETE FROM order_item;
DELETE FROM purchase_order;
INSERT INTO purchase_order (id, tenant_id, status, total) VALUES (1, 7, 'OPEN', 100.00);
INSERT INTO purchase_order (id, tenant_id, status, total) VALUES (2, 8, 'OPEN', 200.00);
INSERT INTO order_item (id, order_id, tenant_id, sku, price) VALUES (10, 1, 7, 'SKU-1', 10.00);
INSERT INTO order_item (id, order_id, tenant_id, sku, price) VALUES (11, 2, 8, 'SKU-2', 20.00);
