/*
 * Copyright 2026 the QueryFence authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.acme.shop.mybatis;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** MyBatis builds the SQL from the XML below, differently for every set of arguments. */
public interface OrderMapper {

  /** Dynamic search: the tenant condition only appears when a tenant is passed. */
  List<Map<String, Object>> search(
      @Param("tenantId") Long tenantId, @Param("statuses") List<String> statuses);

  List<Map<String, Object>> withItems(@Param("tenantId") Long tenantId);

  List<Map<String, Object>> withItemsUnfenced(@Param("tenantId") Long tenantId);

  int closeOrders(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);
}
