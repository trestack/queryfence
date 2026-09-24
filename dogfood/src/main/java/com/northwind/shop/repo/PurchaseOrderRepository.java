package com.northwind.shop.repo;

import com.northwind.shop.domain.PurchaseOrder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository
    extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {

  List<PurchaseOrder> findByTenantIdAndStatus(Long tenantId, String status);

  List<PurchaseOrder> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

  Page<PurchaseOrder> findByTenantIdAndDeletedAtIsNull(Long tenantId, Pageable pageable);

  long countByTenantIdAndStatus(Long tenantId, String status);

  Optional<PurchaseOrder> findByTenantIdAndId(Long tenantId, Long id);

  @Query("select o from PurchaseOrder o join fetch o.items"
      + " where o.tenantId = :tenantId and o.status = :status")
  List<PurchaseOrder> findWithItems(@Param("tenantId") Long tenantId,
      @Param("status") String status);

  @Query("select o from PurchaseOrder o join Customer c on c.id = o.customerId"
      + " where o.tenantId = :tenantId and c.tenantId = :tenantId and c.status = 'ACTIVE'")
  List<PurchaseOrder> findOfActiveCustomers(@Param("tenantId") Long tenantId);

  /** Same list, written by a different hand: the customer side lost its tenant filter. */
  @Query("select o from PurchaseOrder o join Customer c on c.id = o.customerId"
      + " where o.tenantId = :tenantId and c.status = 'ACTIVE' order by o.createdAt desc")
  List<PurchaseOrder> findOfActiveCustomersSorted(@Param("tenantId") Long tenantId);

  @Query("select o from PurchaseOrder o where o.tenantId = :tenantId"
      + " and exists (select 1 from Invoice i where i.orderId = o.id and i.tenantId = o.tenantId"
      + "   and i.status = 'OPEN')")
  List<PurchaseOrder> findWithOpenInvoice(@Param("tenantId") Long tenantId);

  @Query("select o.status, sum(o.total) from PurchaseOrder o where o.tenantId = :tenantId"
      + " group by o.status having sum(o.total) > :floor")
  List<Object[]> totalsByStatus(@Param("tenantId") Long tenantId,
      @Param("floor") BigDecimal floor);

  @Query(value = "select o.id, o.total, i.number from purchase_order o"
      + " join invoice i on i.order_id = o.id and i.tenant_id = o.tenant_id"
      + " where o.tenant_id = :tenantId and i.status = :status", nativeQuery = true)
  List<Object[]> invoicedOrders(@Param("tenantId") Long tenantId, @Param("status") String status);

  /** The dunning screen. The join to invoice was added later and nobody re-read the WHERE. */
  @Query(value = "select o.id, o.total, i.number, i.issued_at from purchase_order o"
      + " join invoice i on i.order_id = o.id"
      + " where o.tenant_id = :tenantId and i.status = 'OPEN' order by i.issued_at",
      nativeQuery = true)
  List<Object[]> overdueOrders(@Param("tenantId") Long tenantId);

  @Modifying
  @Query("update PurchaseOrder o set o.status = 'CLOSED' where o.tenantId = :tenantId"
      + " and o.status = 'OPEN' and o.createdAt < :before")
  int closeStale(@Param("tenantId") Long tenantId, @Param("before") Instant before);

  /** Currency migration, run once per deployment from an admin endpoint. */
  @Modifying
  @Query("update PurchaseOrder o set o.currencyCode = :to where o.currencyCode = :from")
  int migrateCurrency(@Param("from") String from, @Param("to") String to);

  // Reporting helper from the first prototype, still called by the status widget.
  List<PurchaseOrder> findByStatus(String status);
}
