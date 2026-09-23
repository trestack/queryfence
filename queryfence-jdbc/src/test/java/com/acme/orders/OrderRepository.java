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
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Stands in for application code under test. Every method records the line it executes SQL on, so
 * the tests can assert the resolved origin without hard-coding line numbers.
 */
public final class OrderRepository {

  /** The line of the last SQL call made by this class. */
  public static int lastLine;

  private final JdbcTemplate jdbc;

  public OrderRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> findByTenantAndStatus(long tenantId, String status) {
    lastLine = here() + 1;
    return jdbc.queryForList(
        "SELECT id, total FROM purchase_order WHERE tenant_id = ? AND status = ?",
        tenantId,
        status);
  }

  /** The leak the README talks about: the tenant is accepted but never used. */
  public List<Map<String, Object>> findByStatus(long tenantId, String status) {
    lastLine = here() + 1;
    return jdbc.queryForList("SELECT id, total FROM purchase_order WHERE status = ?", status);
  }

  public int closeAll() {
    lastLine = here() + 1;
    return jdbc.update("UPDATE purchase_order SET status = 'CLOSED'");
  }

  public int[] insertAll(List<Object[]> rows) {
    lastLine = here() + 1;
    return jdbc.batchUpdate(
        "INSERT INTO purchase_order (id, tenant_id, status, total) VALUES (?, ?, ?, ?)", rows);
  }

  /** Runs the leaking query through a lambda, so the stack has framework frames in between. */
  public List<Map<String, Object>> findByStatusInCallback(String status) {
    lastLine = here() + 1;
    return jdbc.execute(
        (org.springframework.jdbc.core.ConnectionCallback<List<Map<String, Object>>>)
            connection -> {
              try (var statement =
                  connection.prepareStatement(
                      "SELECT id, total FROM purchase_order WHERE status = '" + status + "'")) {
                statement.executeQuery().close();
              }
              return List.of();
            });
  }

  /** Fails while preparing: H2 rejects the unknown table before the statement is executed. */
  public void readMissingTable() {
    lastLine = here() + 1;
    jdbc.queryForList("SELECT id FROM missing_table WHERE tenant_id = ?", 1L);
  }

  /**
   * Fails while executing: the row already exists, so the driver reports a constraint violation.
   */
  public void insertFixedRow() {
    lastLine = here() + 1;
    jdbc.update(
        "INSERT INTO purchase_order (id, tenant_id, status, total) VALUES (?, ?, ?, ?)",
        42L,
        7L,
        "OPEN",
        1);
  }

  private static int here() {
    return StackWalker.getInstance()
        .walk(frames -> frames.skip(1).findFirst().orElseThrow())
        .getLineNumber();
  }
}
