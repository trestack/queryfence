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
package com.acme.shop.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Wraps the repository so each call has an origin of its own in the report. */
@Service
public class JpaOrderService {

  private final OrderRepository orders;

  @PersistenceContext private EntityManager entityManager;

  public JpaOrderService(OrderRepository orders) {
    this.orders = orders;
  }

  public List<PurchaseOrder> derivedWithTenant(long tenantId) {
    return orders.findByTenantIdAndStatus(tenantId, "OPEN");
  }

  public List<PurchaseOrder> derivedWithoutTenant() {
    return orders.findByStatus("OPEN");
  }

  public Page<PurchaseOrder> pagedWithTenant(long tenantId) {
    return orders.findByTenantId(tenantId, PageRequest.of(0, 10));
  }

  public Page<PurchaseOrder> pagedWithoutTenant() {
    return orders.findByStatusOrderByIdAsc("OPEN", PageRequest.of(0, 10));
  }

  public List<PurchaseOrder> jpqlWithTenant(long tenantId) {
    return orders.jpqlByTenant(tenantId, 1.0);
  }

  public List<PurchaseOrder> jpqlWithoutTenant() {
    return orders.jpqlByTotal(1.0);
  }

  @Transactional(readOnly = true)
  public List<PurchaseOrder> fetchJoin(long tenantId) {
    return orders.fetchItemsOfTenant(tenantId);
  }

  public List<PurchaseOrder> subqueryWithTenant(long tenantId) {
    return orders.withItemsOfTenant(tenantId);
  }

  /** {@code EntityManager.find} loads by primary key only: no tenant anywhere. */
  @Transactional(readOnly = true)
  public PurchaseOrder loadById(long id) {
    return entityManager.find(PurchaseOrder.class, id);
  }

  /** Lazily loading the items of an order makes Hibernate select them by foreign key. */
  @Transactional(readOnly = true)
  public int countItemsOf(long tenantId, long orderId) {
    PurchaseOrder order =
        orders.findByTenantIdAndStatus(tenantId, "OPEN").stream()
            .filter(candidate -> candidate.getId() == orderId)
            .findFirst()
            .orElseThrow();
    return order.getItems().size();
  }
}
