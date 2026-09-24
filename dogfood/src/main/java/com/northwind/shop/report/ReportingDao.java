package com.northwind.shop.report;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Reports. Hand-written SQL on purpose: these queries are read by the finance team. */
@Repository
public class ReportingDao {

  private final JdbcTemplate jdbc;

  public ReportingDao(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> dailyRevenue(long tenantId) {
    return jdbc.queryForList(
        "select date_trunc('day', o.created_at) as day, sum(o.total) as revenue"
            + " from purchase_order o"
            + " where o.tenant_id = ? and o.deleted_at is null"
            + " group by date_trunc('day', o.created_at) order by day",
        tenantId);
  }

  public List<Map<String, Object>> topCustomers(long tenantId, BigDecimal floor) {
    return jdbc.queryForList(
        "select c.name, sum(o.total) as spent from customer c"
            + " join purchase_order o on o.customer_id = c.id and o.tenant_id = c.tenant_id"
            + " where c.tenant_id = ?"
            + " group by c.name having sum(o.total) >= ? order by spent desc",
        tenantId, floor);
  }

  public List<Map<String, Object>> currencyRates() {
    return jdbc.queryForList("select code, rate from currency order by code");
  }

  /** Invoice ageing buckets. The tenant filter lives in the CTE, where the rows come from. */
  public List<Map<String, Object>> invoiceAgeing(long tenantId) {
    return jdbc.queryForList(
        "with issued as ("
            + "  select i.id, i.amount, i.issued_at from invoice i where i.tenant_id = ?"
            + ") select count(*) as invoices, sum(amount) as amount from issued",
        tenantId);
  }

  /**
   * Same report for the order list. Here the tenant filter ended up outside the subquery, which
   * reads just as safely to a human.
   */
  public List<Map<String, Object>> orderTotalsFromSubquery(long tenantId) {
    return jdbc.queryForList(
        "select t.status, sum(t.total) as total from"
            + " (select o.status, o.total, o.tenant_id from purchase_order o"
            + "  where o.deleted_at is null) t"
            + " where t.tenant_id = ? group by t.status",
        tenantId);
  }

  /** The finance dashboard: paid and open amounts in one result. */
  public List<Map<String, Object>> settlementSummary(long tenantId) {
    return jdbc.queryForList(
        "select 'paid' as bucket, coalesce(sum(p.amount), 0) as amount from payment p"
            + " where p.tenant_id = ?"
            + " union all"
            + " select 'open' as bucket, coalesce(sum(i.amount), 0) as amount from invoice i"
            + " where i.status = 'OPEN'",
        tenantId);
  }

  /** Platform-wide revenue for the operator dashboard. Deliberately across every tenant. */
  public List<Map<String, Object>> platformRevenue() {
    return jdbc.queryForList(
        "select o.tenant_id, sum(o.total) as revenue from purchase_order o"
            + " where o.deleted_at is null group by o.tenant_id order by revenue desc");
  }

  public int markInvoicesPaid(long tenantId, List<Long> invoiceIds) {
    String placeholders = String.join(",", invoiceIds.stream().map(id -> "?").toList());
    Object[] args = new Object[invoiceIds.size() + 1];
    for (int i = 0; i < invoiceIds.size(); i++) {
      args[i] = invoiceIds.get(i);
    }
    args[invoiceIds.size()] = tenantId;
    return jdbc.update(
        "update invoice set status = 'PAID' where id in (" + placeholders + ") and tenant_id = ?",
        args);
  }

  /** Fixture-style maintenance the ops team runs from a test-only endpoint. */
  public int renumberInvoices(String prefix) {
    return jdbc.update(
        "update invoice set number = cast(? as varchar) || '-' || cast(id as varchar)"
            + " where number is null",
        prefix);
  }
}
