/*
 * Copyright 2026 the QueryFence authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.acme.shop.jdbc;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Hand-written SQL, the simplest case: what the developer writes is what the database sees. */
@Component
public class JdbcOrderDao {

  private final JdbcTemplate jdbc;

  public JdbcOrderDao(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> byTenantAndStatus(long tenantId, String status) {
    return jdbc.queryForList(
        "SELECT id, total FROM purchase_order WHERE tenant_id = ? AND status = ?",
        tenantId,
        status);
  }

  /** Leak: the tenant is accepted and ignored. */
  public List<Map<String, Object>> byStatus(long tenantId, String status) {
    return jdbc.queryForList("SELECT id, total FROM purchase_order WHERE status = ?", status);
  }

  public List<Map<String, Object>> joinItems(long tenantId) {
    return jdbc.queryForList(
        "SELECT o.id, i.sku FROM purchase_order o"
            + " JOIN order_item i ON i.order_id = o.id AND i.tenant_id = o.tenant_id"
            + " WHERE o.tenant_id = ?",
        tenantId);
  }

  /** Leak: the joined table is not filtered by tenant. */
  public List<Map<String, Object>> joinItemsWithoutTenant(long tenantId) {
    return jdbc.queryForList(
        "SELECT o.id, i.sku FROM purchase_order o"
            + " JOIN order_item i ON i.order_id = o.id"
            + " WHERE o.tenant_id = ?",
        tenantId);
  }

  public int closeOrder(long tenantId, long id) {
    return jdbc.update(
        "UPDATE purchase_order SET status = 'CLOSED' WHERE tenant_id = ? AND id = ?", tenantId, id);
  }

  /** Leak: an UPDATE that touches every tenant's rows. */
  public int closeEverything() {
    return jdbc.update("UPDATE purchase_order SET status = 'CLOSED'");
  }
}
