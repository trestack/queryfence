package com.acme.shop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Ordinary tests: nothing here mentions QueryFence. The second one fails, because the query it
 * runs reads every tenant's orders. Fix OrderMapper.xml and the build goes green.
 */
@SpringBootTest
class OrderServiceTest {

  @Autowired OrderService service;

  @Test
  void listsTheOrdersOfOneTenant() {
    assertThat(service.ordersOf(7L)).hasSize(1);
  }

  @Test
  void listsTheOpenOrdersOfOneTenant() {
    // This assertion passes: the fixture happens to have one OPEN order, and it belongs to
    // tenant 7. The query behind it ignores the tenant, and QueryFence fails the test for it.
    assertThat(service.openOrdersOf(7L)).hasSize(1);
  }
}
