DELETE FROM invoice;
-- Tenant 7 has the only OPEN invoice. The fixture does not expose the leak, so the tests below
-- pass on their data and would stay green for years. QueryFence fails the build anyway.
INSERT INTO invoice (id, tenant_id, status, amount) VALUES (1, 7, 'OPEN', 100.00);
INSERT INTO invoice (id, tenant_id, status, amount) VALUES (2, 8, 'PAID', 200.00);
