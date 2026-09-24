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
package com.acme.orders;

import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** Stands in for application code; each method records the line it runs SQL on. */
public final class OrderRepository {

  /** The line of the last SQL call made by this class. */
  public static int lastLine;

  private final JdbcTemplate jdbc;

  public OrderRepository(DataSource dataSource) {
    this.jdbc = new JdbcTemplate(dataSource);
  }

  public void createSchema() {
    jdbc.execute(
        "CREATE TABLE purchase_order (id BIGINT PRIMARY KEY, tenant_id BIGINT,"
            + " status VARCHAR(32), number VARCHAR(32))");
  }

  public void insert(long id, long tenantId, String status) {
    jdbc.update(
        "INSERT INTO purchase_order (id, tenant_id, status) VALUES (?, ?, ?)",
        id,
        tenantId,
        status);
  }

  public List<Map<String, Object>> findByTenantAndStatus(long tenantId, String status) {
    lastLine = here() + 1;
    return jdbc.queryForList(
        "SELECT id FROM purchase_order WHERE tenant_id = ? AND status = ?", tenantId, status);
  }

  /** The leak: the tenant is accepted but never used. */
  public List<Map<String, Object>> findByStatus(long tenantId, String status) {
    lastLine = here() + 1;
    return jdbc.queryForList("SELECT id FROM purchase_order WHERE status = ?", status);
  }

  /**
   * Valid SQL that JSqlParser 5.4 cannot read, because {@code number} is unqualified. The statement
   * is properly fenced, so it is the shape of a false positive: QueryFence can only say it could
   * not check it.
   */
  public int renumber(long tenantId) {
    lastLine = here() + 1;
    return jdbc.update(
        "UPDATE purchase_order SET number = 'INV' WHERE number IS NULL AND tenant_id = ?",
        tenantId);
  }

  public int closeAll() {
    lastLine = here() + 1;
    return jdbc.update("UPDATE purchase_order SET status = 'CLOSED'");
  }

  private static int here() {
    return StackWalker.getInstance()
        .walk(frames -> frames.skip(1).findFirst().orElseThrow())
        .getLineNumber();
  }
}
