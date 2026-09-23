package com.acme.billing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Ordinary tests: nothing here mentions QueryFence. The second one fails, because Hibernate turns
 * findByStatus into a query without a tenant filter. Delete that repository method, or add the
 * tenant to it, and the build goes green.
 */
@SpringBootTest
class InvoiceServiceTest {

  @Autowired InvoiceService service;

  @Test
  void listsTheOpenInvoicesOfOneTenant() {
    assertThat(service.openInvoicesOf(7L)).hasSize(1);
  }

  @Test
  void listsOpenInvoices() {
    assertThat(service.openInvoices()).hasSize(2);
  }
}
