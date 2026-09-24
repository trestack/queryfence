package com.northwind.shop.service;

import com.northwind.shop.mapper.OrderSearchMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The order search screen, backed by MyBatis. */
@Service
@Transactional(readOnly = true)
public class OrderSearchService {

  private final OrderSearchMapper mapper;

  public OrderSearchService(OrderSearchMapper mapper) {
    this.mapper = mapper;
  }

  public List<Map<String, Object>> search(long tenantId, String status, BigDecimal minTotal) {
    return mapper.search(tenantId, status, minTotal);
  }

  /**
   * The same screen in the operator back-office, which searches across tenants by passing no
   * tenant at all.
   */
  public List<Map<String, Object>> searchAnyTenant(String status) {
    return mapper.search(null, status, null);
  }

  public List<Map<String, Object>> withInvoice(long tenantId, String invoiceStatus) {
    return mapper.searchWithInvoice(tenantId, invoiceStatus);
  }

  public List<Map<String, Object>> countByStatus(long tenantId) {
    return mapper.countByStatus(tenantId);
  }

  public List<Map<String, Object>> itemsOf(long tenantId, List<Long> orderIds) {
    return mapper.itemsOfOrders(tenantId, orderIds);
  }

  public List<Map<String, Object>> exportRows(long tenantId) {
    return mapper.exportRows(tenantId);
  }
}
