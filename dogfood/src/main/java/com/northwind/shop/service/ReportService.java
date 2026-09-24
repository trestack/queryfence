package com.northwind.shop.service;

import com.northwind.shop.report.ReportingDao;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

  private final ReportingDao dao;

  public ReportService(ReportingDao dao) {
    this.dao = dao;
  }

  public List<Map<String, Object>> dailyRevenue(long tenantId) {
    return dao.dailyRevenue(tenantId);
  }

  public List<Map<String, Object>> topCustomers(long tenantId, BigDecimal floor) {
    return dao.topCustomers(tenantId, floor);
  }

  public List<Map<String, Object>> invoiceAgeing(long tenantId) {
    return dao.invoiceAgeing(tenantId);
  }

  public List<Map<String, Object>> orderTotals(long tenantId) {
    return dao.orderTotalsFromSubquery(tenantId);
  }

  public List<Map<String, Object>> settlement(long tenantId) {
    return dao.settlementSummary(tenantId);
  }

  public List<Map<String, Object>> currencies() {
    return dao.currencyRates();
  }

  /** Operator-only: revenue per tenant for the platform dashboard. */
  public List<Map<String, Object>> platformRevenue() {
    return dao.platformRevenue();
  }
}
