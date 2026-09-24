package com.northwind.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.northwind.shop.domain.Customer;
import com.northwind.shop.service.CustomerService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

class CustomerQueriesTest extends AbstractShopTest {

  @Autowired CustomerService customers;

  @Test
  void listsCustomersWithAStatus() {
    List<Customer> found = customers.withStatus(TENANT, "ACTIVE");

    assertThat(found).extracting(Customer::getName).containsExactly("Acme Industries");
  }

  @Test
  void findsACustomerByEmail() {
    assertThat(customers.byEmail(TENANT, "ops@acme.test")).isPresent();
    assertThat(customers.byEmail(TENANT, "ops@globex.test")).isEmpty();
  }

  @Test
  void pagesThroughTheCustomerList() {
    Page<Customer> page = customers.page(TENANT, 0, 10);

    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.getContent()).extracting(Customer::getName)
        .containsExactly("Acme Industries", "Acme Retail");
  }

  @Test
  void listsActiveCustomersSortedByName() {
    assertThat(customers.active(TENANT)).hasSize(2);
  }

  @Test
  void searchesByName() {
    assertThat(customers.search(TENANT, "acme")).hasSize(2);
    assertThat(customers.search(TENANT, "globex")).isEmpty();
  }

  @Test
  void countsByStatus() {
    assertThat(customers.countWithStatus(TENANT, "CLOSED")).isEqualTo(1);
  }

  @Test
  void registersANewCustomer() {
    Customer saved = customers.register(TENANT, "Acme Labs", "labs@acme.test");

    assertThat(saved.getId()).isNotNull();
    assertThat(customers.countWithStatus(TENANT, "ACTIVE")).isEqualTo(2);
  }

  @Test
  void closesACustomerAndArchivesIt() {
    assertThat(customers.close(TENANT, 1L)).isEqualTo(1);
    assertThat(customers.archiveClosed()).isEqualTo(1);
  }

  @Test
  void looksUpACustomerById() {
    assertThat(customers.byId(1L)).isPresent();
  }

  @Test
  void listsClosedCustomersForTheChurnReport() {
    assertThat(customers.closedEverywhere()).hasSize(2);
  }
}
