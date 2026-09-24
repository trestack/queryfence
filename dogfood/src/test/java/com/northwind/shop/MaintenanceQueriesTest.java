package com.northwind.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.northwind.shop.service.MaintenanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MaintenanceQueriesTest extends AbstractShopTest {

  @Autowired MaintenanceService maintenance;

  @Test
  void renumbersInvoicesThatHaveNoNumber() {
    assertThat(maintenance.renumberInvoices("INV")).isZero();
  }
}
