package com.northwind.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.northwind.shop.domain.Invoice;
import com.northwind.shop.service.BillingService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BillingQueriesTest extends AbstractShopTest {

  @Autowired BillingService billing;

  @Test
  void listsInvoicesWithAStatus() {
    assertThat(billing.withStatus(TENANT, "OPEN")).extracting(Invoice::getNumber)
        .containsExactly("INV-7-001");
  }

  @Test
  void pagesThroughInvoices() {
    assertThat(billing.page(TENANT, 0, 10).getTotalElements()).isEqualTo(2);
  }

  @Test
  void listsInvoicesWithoutAPayment() {
    assertThat(billing.unpaid(TENANT)).extracting(Invoice::getNumber)
        .containsExactly("INV-7-001");
  }

  @Test
  void aggregatesInvoiceTotalsByStatus() {
    assertThat(billing.statusTotals(TENANT)).hasSize(2);
  }

  @Test
  void listsThePaymentsOfAnInvoice() {
    assertThat(billing.paymentsOf(TENANT, 501L)).hasSize(1);
    assertThat(billing.paymentsOf(TENANT, 502L)).isEmpty();
  }

  @Test
  void aggregatesPaymentsByMethod() {
    assertThat(billing.paymentTotals(TENANT)).hasSize(1);
  }

  @Test
  void marksInvoicesPaid() {
    assertThat(billing.markPaid(TENANT, List.of(500L))).isEqualTo(1);
  }

  @Test
  void findsAnInvoiceByNumberForTheWebhook() {
    assertThat(billing.byNumber("INV-7-001")).isPresent();
  }

  @Test
  void listsPaymentsForReconciliation() {
    assertThat(billing.allPayments()).hasSize(2);
  }
}
