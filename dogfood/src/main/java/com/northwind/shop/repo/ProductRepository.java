package com.northwind.shop.repo;

import com.northwind.shop.domain.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

  List<Product> findByCategoryOrderByNameAsc(String category);

  @Query(value = "select p.sku, sum(i.quantity) from product p"
      + " join order_item i on i.product_id = p.id and i.tenant_id = :tenantId"
      + " group by p.sku order by sum(i.quantity) desc limit 5", nativeQuery = true)
  List<Object[]> bestSellers(@Param("tenantId") Long tenantId);
}
