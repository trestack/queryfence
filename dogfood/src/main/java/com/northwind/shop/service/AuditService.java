package com.northwind.shop.service;

import com.northwind.shop.domain.AuditLog;
import com.northwind.shop.repo.AuditLogRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditService {

  private final AuditLogRepository log;

  public AuditService(AuditLogRepository log) {
    this.log = log;
  }

  public AuditLog record(long tenantId, String actor, String action, String entity, long entityId) {
    return log.save(new AuditLog(tenantId, actor, action, entity, entityId));
  }

  /** Used by the bulk import, which writes thousands of rows per run. */
  public void recordFast(String actor, String action, String entity, long entityId) {
    log.recordFast(actor, action, entity, entityId);
  }

  public List<AuditLog> history(long tenantId) {
    return log.findByTenantIdOrderByAtDesc(tenantId);
  }
}
