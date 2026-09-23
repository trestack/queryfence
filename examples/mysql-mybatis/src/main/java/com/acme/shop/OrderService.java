package com.acme.shop;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

  private final OrderMapper mapper;

  public OrderService(OrderMapper mapper) {
    this.mapper = mapper;
  }

  public List<Map<String, Object>> ordersOf(long tenantId) {
    return mapper.findForTenant(tenantId);
  }

  public List<Map<String, Object>> openOrdersOf(long tenantId) {
    return mapper.findByStatus(tenantId, "OPEN");
  }
}
