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

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Every method here makes Hibernate write the SQL, which is the point of these tests. */
public interface OrderRepository extends JpaRepository<PurchaseOrder, Long> {

  /** Derived query with the tenant. */
  List<PurchaseOrder> findByTenantIdAndStatus(Long tenantId, String status);

  /** Derived query without the tenant: a leak. */
  List<PurchaseOrder> findByStatus(String status);

  /** Derived query with the tenant, paginated: Hibernate adds LIMIT/OFFSET and a count query. */
  Page<PurchaseOrder> findByTenantId(Long tenantId, Pageable pageable);

  /** Derived query without the tenant, paginated: a leak in both the page and the count query. */
  Page<PurchaseOrder> findByStatusOrderByIdAsc(String status, Pageable pageable);

  @Query("select o from PurchaseOrder o where o.tenantId = :tenantId and o.total > :min")
  List<PurchaseOrder> jpqlByTenant(@Param("tenantId") Long tenantId, @Param("min") double min);

  /** JPQL without the tenant: a leak. */
  @Query("select o from PurchaseOrder o where o.total > :min")
  List<PurchaseOrder> jpqlByTotal(@Param("min") double min);

  /** Fetch join: Hibernate joins order_item without a tenant condition of its own. */
  @Query("select distinct o from PurchaseOrder o join fetch o.items where o.tenantId = :tenantId")
  List<PurchaseOrder> fetchItemsOfTenant(@Param("tenantId") Long tenantId);

  /** Subquery in JPQL, with the tenant on both levels. */
  @Query(
      "select o from PurchaseOrder o where o.tenantId = :tenantId and exists"
          + " (select i.id from OrderItem i where i.orderId = o.id and i.tenantId = :tenantId)")
  List<PurchaseOrder> withItemsOfTenant(@Param("tenantId") Long tenantId);
}
