DELETE FROM audit_log; DELETE FROM payment; DELETE FROM invoice; DELETE FROM order_item;
DELETE FROM purchase_order; DELETE FROM customer; DELETE FROM product; DELETE FROM currency;

INSERT INTO currency (code, rate) VALUES ('USD', 1.000000), ('EUR', 0.920000);
INSERT INTO product (id, sku, name, category, price) VALUES
  (1, 'SKU-KEY', 'Mechanical keyboard', 'PERIPHERALS', 120.00),
  (2, 'SKU-MON', '27 inch monitor', 'DISPLAYS', 320.00),
  (3, 'SKU-DCK', 'Docking station', 'PERIPHERALS', 180.00);

INSERT INTO customer (id, tenant_id, name, email, status, deleted_at) VALUES
  (1, 7, 'Acme Industries', 'ops@acme.test', 'ACTIVE', NULL),
  (2, 7, 'Acme Retail', 'retail@acme.test', 'CLOSED', NULL),
  (3, 8, 'Globex', 'ops@globex.test', 'ACTIVE', NULL),
  (4, 8, 'Globex Legacy', 'legacy@globex.test', 'CLOSED', NULL);

INSERT INTO purchase_order (id, tenant_id, customer_id, status, total, currency_code, created_at, deleted_at) VALUES
  (100, 7, 1, 'OPEN', 440.00, 'USD', '2026-01-15 10:00:00', NULL),
  (101, 7, 1, 'CLOSED', 120.00, 'USD', '2026-01-15 11:00:00', NULL),
  (102, 8, 3, 'OPEN', 320.00, 'EUR', '2026-01-16 09:00:00', NULL);

INSERT INTO order_item (id, tenant_id, order_id, product_id, quantity, price) VALUES
  (1000, 7, 100, 1, 1, 120.00),
  (1001, 7, 100, 2, 1, 320.00),
  (1002, 7, 101, 1, 1, 120.00),
  (1003, 8, 102, 2, 1, 320.00);

INSERT INTO invoice (id, tenant_id, order_id, number, status, amount) VALUES
  (500, 7, 100, 'INV-7-001', 'OPEN', 440.00),
  (501, 7, 101, 'INV-7-002', 'PAID', 120.00),
  (502, 8, 102, 'INV-8-001', 'OPEN', 320.00);

INSERT INTO payment (id, tenant_id, invoice_id, amount, method) VALUES
  (900, 7, 501, 120.00, 'CARD'),
  (901, 8, 502, 320.00, 'BANK');
