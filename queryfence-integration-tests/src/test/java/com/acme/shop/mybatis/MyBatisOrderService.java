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
package com.acme.shop.mybatis;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Each method is one origin in the report. */
@Service
public class MyBatisOrderService {

  private final OrderMapper mapper;

  public MyBatisOrderService(OrderMapper mapper) {
    this.mapper = mapper;
  }

  public List<Map<String, Object>> searchWithTenant(long tenantId) {
    return mapper.search(tenantId, List.of("OPEN", "CLOSED"));
  }

  /** The same mapper method, called without a tenant: the <if> drops the condition. */
  public List<Map<String, Object>> searchWithoutTenant() {
    return mapper.search(null, List.of("OPEN", "CLOSED"));
  }

  public List<Map<String, Object>> searchWithTenantOnly(long tenantId) {
    return mapper.search(tenantId, List.of());
  }

  public List<Map<String, Object>> joinedWithTenant(long tenantId) {
    return mapper.withItems(tenantId);
  }

  public List<Map<String, Object>> joinedWithoutItemTenant(long tenantId) {
    return mapper.withItemsUnfenced(tenantId);
  }

  public int closeWithTenant(long tenantId, List<Long> ids) {
    return mapper.closeOrders(tenantId, ids);
  }

  /** No tenant and no ids: the <where> disappears and the UPDATE hits every row. */
  public int closeEverything() {
    return mapper.closeOrders(null, List.of());
  }
}
