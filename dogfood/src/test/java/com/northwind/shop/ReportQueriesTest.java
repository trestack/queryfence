package com.northwind.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.northwind.shop.service.ReportService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReportQueriesTest extends AbstractShopTest {

  @Autowired ReportService reports;

  @Test
  void reportsRevenuePerDay() {
    assertThat(reports.dailyRevenue(TENANT)).hasSize(1);
  }

  @Test
  void reportsTheBestCustomers() {
    assertThat(reports.topCustomers(TENANT, new BigDecimal("100"))).hasSize(1);
  }

  @Test
  void reportsInvoiceAgeing() {
    assertThat(reports.invoiceAgeing(TENANT)).hasSize(1);
    assertThat(reports.invoiceAgeing(TENANT).get(0).get("invoices")).isEqualTo(2L);
  }

  @Test
  void reportsOrderTotals() {
    assertThat(reports.orderTotals(TENANT)).hasSize(2);
  }

  @Test
  void reportsSettlement() {
    assertThat(reports.settlement(TENANT)).hasSize(2);
  }

  @Test
  void readsTheCurrencyTable() {
    assertThat(reports.currencies()).hasSize(2);
  }

  @Test
  void reportsRevenuePerTenantForTheOperator() {
    assertThat(reports.platformRevenue()).hasSize(2);
  }
}
