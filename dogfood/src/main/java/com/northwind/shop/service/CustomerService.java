package com.northwind.shop.service;

import com.northwind.shop.domain.Customer;
import com.northwind.shop.repo.CustomerRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerService {

  private final CustomerRepository customers;

  public CustomerService(CustomerRepository customers) {
    this.customers = customers;
  }

  public List<Customer> withStatus(long tenantId, String status) {
    return customers.findByTenantIdAndStatus(tenantId, status);
  }

  public Optional<Customer> byEmail(long tenantId, String email) {
    return customers.findByTenantIdAndEmail(tenantId, email);
  }

  public Page<Customer> page(long tenantId, int page, int size) {
    return customers.findByTenantIdAndDeletedAtIsNull(
        tenantId, PageRequest.of(page, size, Sort.by("name").ascending()));
  }

  public List<Customer> active(long tenantId) {
    return customers.listActive(tenantId);
  }

  public List<Customer> search(long tenantId, String term) {
    return customers.searchByName(tenantId, term);
  }

  public long countWithStatus(long tenantId, String status) {
    return customers.countByTenantIdAndStatus(tenantId, status);
  }

  public Customer register(long tenantId, String name, String email) {
    return customers.save(new Customer(tenantId, name, email, "ACTIVE"));
  }

  public int close(long tenantId, long customerId) {
    return customers.softDelete(tenantId, customerId, Instant.now());
  }

  public int archiveClosed() {
    return customers.archiveDeleted();
  }

  /** The support console: an operator pastes a customer id from a ticket. */
  public Optional<Customer> byId(long customerId) {
    return customers.findById(customerId);
  }

  /** The support console again: every closed customer, for the churn spreadsheet. */
  public List<Customer> closedEverywhere() {
    return customers.findByStatusOrderByNameAsc("CLOSED");
  }
}
