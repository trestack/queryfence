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
package com.acme.shop;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/** Application code; each query method records the line it runs SQL on. */
public class OrderRepository {

  /** The line of the last SQL call made by this class. */
  public static int lastLine;

  private final JdbcTemplate jdbc;

  public OrderRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
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

  private static int here() {
    return StackWalker.getInstance()
        .walk(frames -> frames.skip(1).findFirst().orElseThrow())
        .getLineNumber();
  }
}
