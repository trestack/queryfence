package com.northwind.shop.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** The order search screen: MyBatis, because the filter combinations outgrew Criteria. */
public interface OrderSearchMapper {

  List<Map<String, Object>> search(@Param("tenantId") Long tenantId,
      @Param("status") String status, @Param("minTotal") java.math.BigDecimal minTotal);

  List<Map<String, Object>> searchWithInvoice(@Param("tenantId") Long tenantId,
      @Param("status") String status);

  List<Map<String, Object>> countByStatus(@Param("tenantId") Long tenantId);

  List<Map<String, Object>> itemsOfOrders(@Param("tenantId") Long tenantId,
      @Param("orderIds") List<Long> orderIds);

  List<Map<String, Object>> exportRows(@Param("tenantId") Long tenantId);
}
