package com.northwind.shop.repo;

import com.northwind.shop.domain.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

  List<OrderItem> findByTenantIdAndOrderId(Long tenantId, Long orderId);

  @Query("select i.productId, sum(i.quantity) from OrderItem i where i.tenantId = :tenantId"
      + " group by i.productId order by sum(i.quantity) desc")
  List<Object[]> quantityPerProduct(@Param("tenantId") Long tenantId);

  @Query(value = "select p.name, sum(i.quantity * i.price) from order_item i"
      + " join product p on p.id = i.product_id"
      + " where i.tenant_id = :tenantId group by p.name having sum(i.quantity) > 0",
      nativeQuery = true)
  List<Object[]> revenuePerProduct(@Param("tenantId") Long tenantId);

  @Modifying
  @Query("delete from OrderItem i where i.tenantId = :tenantId and i.orderId = :orderId")
  int deleteOfOrder(@Param("tenantId") Long tenantId, @Param("orderId") Long orderId);

  // Called from the order detail view, which has already loaded the order.
  List<OrderItem> findByOrderId(Long orderId);
}
