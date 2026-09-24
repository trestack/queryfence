package com.northwind.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.northwind.shop.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CatalogQueriesTest extends AbstractShopTest {

  @Autowired CatalogService catalog;

  @Test
  void listsTheWholeCatalogue() {
    assertThat(catalog.all()).hasSize(3);
  }

  @Test
  void listsOneCategory() {
    assertThat(catalog.inCategory("PERIPHERALS")).hasSize(2);
  }

  @Test
  void listsTheBestSellersOfATenant() {
    assertThat(catalog.bestSellers(TENANT)).hasSize(2);
  }
}
