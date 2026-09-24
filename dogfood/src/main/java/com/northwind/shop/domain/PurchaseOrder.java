package com.northwind.shop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_order")
public class PurchaseOrder {

  @Id private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "customer_id", nullable = false)
  private Long customerId;

  private String status;

  private BigDecimal total;

  @Column(name = "currency_code")
  private String currencyCode;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @OneToMany(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id")
  private List<OrderItem> items = new ArrayList<>();

  protected PurchaseOrder() {}

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public String getStatus() {
    return status;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public List<OrderItem> getItems() {
    return items;
  }
}
