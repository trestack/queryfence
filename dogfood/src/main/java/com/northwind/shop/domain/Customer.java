package com.northwind.shop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "customer")
public class Customer {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_seq")
  @SequenceGenerator(name = "customer_seq", sequenceName = "customer_seq", allocationSize = 50)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  private String name;

  private String email;

  private String status;

  @Column(name = "created_at")
  private Instant createdAt = Instant.now();

  @Column(name = "deleted_at")
  private Instant deletedAt;

  protected Customer() {}

  public Customer(Long tenantId, String name, String email, String status) {
    this.tenantId = tenantId;
    this.name = name;
    this.email = email;
    this.status = status;
  }

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }
}
