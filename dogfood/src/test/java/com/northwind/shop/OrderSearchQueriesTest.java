package com.northwind.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.northwind.shop.service.OrderSearchService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrderSearchQueriesTest extends AbstractShopTest {

  @Autowired OrderSearchService search;

  @Test
  void searchesOrdersOfTheTenant() {
    assertThat(search.search(TENANT, "OPEN", new BigDecimal("100"))).hasSize(1);
  }

  @Test
  void searchesOrdersWithoutAStatusFilter() {
    assertThat(search.search(TENANT, null, null)).hasSize(2);
  }

  @Test
  void searchesFromTheOperatorBackOffice() {
    assertThat(search.searchAnyTenant("OPEN")).hasSize(2);
  }

  @Test
  void joinsTheInvoiceOfEachOrder() {
    assertThat(search.withInvoice(TENANT, "OPEN")).hasSize(1);
  }

  @Test
  void countsOrdersByStatus() {
    assertThat(search.countByStatus(TENANT)).hasSize(2);
  }

  @Test
  void listsTheItemsOfSeveralOrders() {
    assertThat(search.itemsOf(TENANT, List.of(100L, 101L))).hasSize(3);
  }

  @Test
  void exportsTheOrderListWithCustomerNames() {
    assertThat(search.exportRows(TENANT)).hasSize(2);
  }
}
