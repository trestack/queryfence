package com.northwind.shop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "invoice")
public class Invoice {

  @Id private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "order_id", nullable = false)
  private Long orderId;

  private String number;

  private String status;

  private BigDecimal amount;

  @Column(name = "issued_at")
  private Instant issuedAt;

  protected Invoice() {}

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public Long getOrderId() {
    return orderId;
  }

  public String getNumber() {
    return number;
  }

  public String getStatus() {
    return status;
  }

  public BigDecimal getAmount() {
    return amount;
  }
}
