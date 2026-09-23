DELETE FROM purchase_order;
-- Tenant 7 has the only OPEN order. The fixture does not expose the leak, so the tests below
-- pass on their data and would stay green for years. QueryFence fails the build anyway.
INSERT INTO purchase_order (id, tenant_id, status, total) VALUES (1, 7, 'OPEN', 100.00);
INSERT INTO purchase_order (id, tenant_id, status, total) VALUES (2, 8, 'CLOSED', 200.00);
