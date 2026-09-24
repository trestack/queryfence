package com.northwind.shop.repo;

import com.northwind.shop.domain.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

  List<AuditLog> findByTenantIdOrderByAtDesc(Long tenantId);

  /**
   * Fast path used by the import job: one statement, no entity. The column list was copied from an
   * older version of the table.
   */
  @Modifying
  @Query(value = "insert into audit_log (id, actor, action, entity, entity_id, at)"
      + " values (nextval('audit_log_seq'), :actor, :action, :entity, :entityId, now())",
      nativeQuery = true)
  void recordFast(@Param("actor") String actor, @Param("action") String action,
      @Param("entity") String entity, @Param("entityId") Long entityId);
}
