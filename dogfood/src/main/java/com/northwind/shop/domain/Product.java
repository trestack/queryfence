package com.northwind.shop.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** A product of the shared catalogue: not tenant-scoped, every tenant sees the same list. */
@Entity
@Table(name = "product")
public class Product {

  @Id private Long id;

  private String sku;

  private String name;

  private String category;

  private BigDecimal price;

  protected Product() {}

  public Long getId() {
    return id;
  }

  public String getSku() {
    return sku;
  }

  public String getName() {
    return name;
  }

  public String getCategory() {
    return category;
  }

  public BigDecimal getPrice() {
    return price;
  }
}
