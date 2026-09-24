package com.northwind.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.northwind.shop.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuditQueriesTest extends AbstractShopTest {

  @Autowired AuditService audit;

  @Test
  void recordsAnEntry() {
    audit.record(TENANT, "ops@acme.test", "ORDER_CLOSED", "purchase_order", 100L);

    assertThat(audit.history(TENANT)).hasSize(1);
  }

  @Test
  void recordsAnEntryOnTheFastPath() {
    audit.recordFast("import-job", "ORDER_IMPORTED", "purchase_order", 100L);

    assertThat(audit.history(TENANT)).isEmpty();
  }
}
