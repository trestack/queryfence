package com.northwind.shop.service;

import com.northwind.shop.domain.OrderItem;
import com.northwind.shop.domain.PurchaseOrder;
import com.northwind.shop.repo.OrderItemRepository;
import com.northwind.shop.repo.PurchaseOrderRepository;
import com.northwind.shop.repo.PurchaseOrderSpecifications;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {

  private final PurchaseOrderRepository orders;
  private final OrderItemRepository items;

  public OrderService(PurchaseOrderRepository orders, OrderItemRepository items) {
    this.orders = orders;
    this.items = items;
  }

  public List<PurchaseOrder> withStatus(long tenantId, String status) {
    return orders.findByTenantIdAndStatus(tenantId, status);
  }

  public List<PurchaseOrder> newestFirst(long tenantId) {
    return orders.findByTenantIdOrderByCreatedAtDesc(tenantId);
  }

  public Page<PurchaseOrder> page(long tenantId, int page, int size) {
    return orders.findByTenantIdAndDeletedAtIsNull(
        tenantId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
  }

  public Optional<PurchaseOrder> find(long tenantId, long orderId) {
    return orders.findByTenantIdAndId(tenantId, orderId);
  }

  public long count(long tenantId, String status) {
    return orders.countByTenantIdAndStatus(tenantId, status);
  }

  public List<PurchaseOrder> withItems(long tenantId, String status) {
    return orders.findWithItems(tenantId, status);
  }

  public List<PurchaseOrder> ofActiveCustomers(long tenantId) {
    return orders.findOfActiveCustomers(tenantId);
  }

  public List<PurchaseOrder> ofActiveCustomersSorted(long tenantId) {
    return orders.findOfActiveCustomersSorted(tenantId);
  }

  public List<PurchaseOrder> withOpenInvoice(long tenantId) {
    return orders.findWithOpenInvoice(tenantId);
  }

  public List<Object[]> totalsByStatus(long tenantId, BigDecimal floor) {
    return orders.totalsByStatus(tenantId, floor);
  }

  public List<Object[]> invoicedOrders(long tenantId, String invoiceStatus) {
    return orders.invoicedOrders(tenantId, invoiceStatus);
  }

  public List<Object[]> overdue(long tenantId) {
    return orders.overdueOrders(tenantId);
  }

  public List<PurchaseOrder> searchByStatusAndValue(long tenantId, String status,
      BigDecimal floor) {
    return orders.findAll(PurchaseOrderSpecifications.ofTenant(tenantId)
        .and(PurchaseOrderSpecifications.withStatus(status))
        .and(PurchaseOrderSpecifications.totalAbove(floor))
        .and(PurchaseOrderSpecifications.notDeleted()));
  }

  /** The support console's free-form search. The tenant is whatever the operator selected. */
  public List<PurchaseOrder> freeSearch(String status, Long customerId) {
    return orders.findAll(PurchaseOrderSpecifications.freeSearch(status, customerId));
  }

  public List<OrderItem> itemsOf(long tenantId, long orderId) {
    return items.findByTenantIdAndOrderId(tenantId, orderId);
  }

  /** The order detail view: the order is already loaded, so the items follow by id. */
  public List<OrderItem> itemsOf(long orderId) {
    return items.findByOrderId(orderId);
  }

  /** Reads a lazy association: Hibernate loads the children by foreign key. */
  public int countItemsLazily(long tenantId, long orderId) {
    return orders.findByTenantIdAndId(tenantId, orderId)
        .map(order -> order.getItems().size())
        .orElse(0);
  }

  public List<Object[]> quantityPerProduct(long tenantId) {
    return items.quantityPerProduct(tenantId);
  }

  public List<Object[]> revenuePerProduct(long tenantId) {
    return items.revenuePerProduct(tenantId);
  }

  public int closeStale(long tenantId, Instant before) {
    return orders.closeStale(tenantId, before);
  }

  public int migrateCurrency(String from, String to) {
    return orders.migrateCurrency(from, to);
  }

  public int removeItems(long tenantId, long orderId) {
    return items.deleteOfOrder(tenantId, orderId);
  }

  /** The dashboard widget that counts open orders on the instance. */
  public List<PurchaseOrder> everythingWithStatus(String status) {
    return orders.findByStatus(status);
  }
}
