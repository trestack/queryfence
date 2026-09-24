package com.northwind.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.northwind.shop.domain.PurchaseOrder;
import com.northwind.shop.service.OrderService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrderQueriesTest extends AbstractShopTest {

  @Autowired OrderService orders;

  @Test
  void listsOrdersWithAStatus() {
    assertThat(orders.withStatus(TENANT, "OPEN")).extracting(PurchaseOrder::getId)
        .containsExactly(100L);
  }

  @Test
  void listsOrdersNewestFirst() {
    assertThat(orders.newestFirst(TENANT)).hasSize(2);
  }

  @Test
  void pagesThroughOrders() {
    assertThat(orders.page(TENANT, 0, 1).getTotalElements()).isEqualTo(2);
  }

  @Test
  void findsAnOrderOfTheTenantOnly() {
    assertThat(orders.find(TENANT, 100L)).isPresent();
    assertThat(orders.find(TENANT, 102L)).isEmpty();
  }

  @Test
  void countsOpenOrders() {
    assertThat(orders.count(TENANT, "OPEN")).isEqualTo(1);
  }

  @Test
  void fetchesOrdersWithTheirItems() {
    List<PurchaseOrder> found = orders.withItems(TENANT, "OPEN");

    assertThat(found).isNotEmpty();
    assertThat(found.get(0).getItems()).hasSize(2);
  }

  @Test
  void listsOrdersOfActiveCustomers() {
    assertThat(orders.ofActiveCustomers(TENANT)).hasSize(2);
  }

  @Test
  void listsOrdersOfActiveCustomersSorted() {
    assertThat(orders.ofActiveCustomersSorted(TENANT)).hasSize(2);
  }

  @Test
  void listsOrdersThatStillHaveAnOpenInvoice() {
    assertThat(orders.withOpenInvoice(TENANT)).extracting(PurchaseOrder::getId)
        .containsExactly(100L);
  }

  @Test
  void aggregatesTotalsByStatus() {
    assertThat(orders.totalsByStatus(TENANT, BigDecimal.ZERO)).hasSize(2);
  }

  @Test
  void joinsOrdersToInvoices() {
    assertThat(orders.invoicedOrders(TENANT, "OPEN")).hasSize(1);
  }

  @Test
  void listsOverdueOrders() {
    assertThat(orders.overdue(TENANT)).hasSize(1);
  }

  @Test
  void searchesWithCriteria() {
    assertThat(orders.searchByStatusAndValue(TENANT, "OPEN", new BigDecimal("100")))
        .extracting(PurchaseOrder::getId).containsExactly(100L);
  }

  @Test
  void searchesFromTheSupportConsole() {
    assertThat(orders.freeSearch("OPEN", null)).hasSize(2);
  }

  @Test
  void listsTheItemsOfAnOrder() {
    assertThat(orders.itemsOf(TENANT, 100L)).hasSize(2);
  }

  @Test
  void listsTheItemsOfAnOrderFromTheDetailView() {
    assertThat(orders.itemsOf(100L)).hasSize(2);
  }

  @Test
  void readsItemsThroughTheLazyAssociation() {
    assertThat(orders.countItemsLazily(TENANT, 100L)).isEqualTo(2);
  }

  @Test
  void aggregatesQuantitiesPerProduct() {
    assertThat(orders.quantityPerProduct(TENANT)).hasSize(2);
  }

  @Test
  void aggregatesRevenuePerProduct() {
    assertThat(orders.revenuePerProduct(TENANT)).hasSize(2);
  }

  @Test
  void closesStaleOrders() {
    assertThat(orders.closeStale(TENANT, Instant.parse("2026-02-01T00:00:00Z"))).isEqualTo(1);
  }

  @Test
  void migratesTheCurrencyOfEveryOrder() {
    assertThat(orders.migrateCurrency("EUR", "USD")).isEqualTo(1);
  }

  @Test
  void removesTheItemsOfAnOrder() {
    assertThat(orders.removeItems(TENANT, 101L)).isEqualTo(1);
  }

  @Test
  void countsOpenOrdersForTheDashboardWidget() {
    assertThat(orders.everythingWithStatus("OPEN")).hasSize(2);
  }
}
