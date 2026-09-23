package com.acme.shop;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

public interface OrderMapper {

  List<Map<String, Object>> findForTenant(@Param("tenantId") Long tenantId);

  /** The bug this example is about: the tenant argument is never used in the SQL. */
  List<Map<String, Object>> findByStatus(
      @Param("tenantId") Long tenantId, @Param("status") String status);
}
