package com.northwind.shop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment")
public class Payment {

  @Id private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "invoice_id", nullable = false)
  private Long invoiceId;

  private BigDecimal amount;

  private String method;

  @Column(name = "paid_at")
  private Instant paidAt;

  protected Payment() {}

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public Long getInvoiceId() {
    return invoiceId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getMethod() {
    return method;
  }
}
