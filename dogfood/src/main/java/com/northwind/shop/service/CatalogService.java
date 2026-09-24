package com.northwind.shop.service;

import com.northwind.shop.domain.Product;
import com.northwind.shop.repo.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The catalogue is shared: {@code product} has no tenant column by design. */
@Service
@Transactional(readOnly = true)
public class CatalogService {

  private final ProductRepository products;

  public CatalogService(ProductRepository products) {
    this.products = products;
  }

  public List<Product> all() {
    return products.findAll();
  }

  public List<Product> inCategory(String category) {
    return products.findByCategoryOrderByNameAsc(category);
  }

  public List<Object[]> bestSellers(long tenantId) {
    return products.bestSellers(tenantId);
  }
}
