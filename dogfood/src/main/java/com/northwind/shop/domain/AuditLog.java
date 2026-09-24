package com.northwind.shop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_log_seq")
  @SequenceGenerator(name = "audit_log_seq", sequenceName = "audit_log_seq", allocationSize = 50)
  private Long id;

  @Column(name = "tenant_id")
  private Long tenantId;

  private String actor;

  private String action;

  private String entity;

  @Column(name = "entity_id")
  private Long entityId;

  @Column(name = "at")
  private Instant at = Instant.now();

  protected AuditLog() {}

  public AuditLog(Long tenantId, String actor, String action, String entity, Long entityId) {
    this.tenantId = tenantId;
    this.actor = actor;
    this.action = action;
    this.entity = entity;
    this.entityId = entityId;
  }

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public String getAction() {
    return action;
  }
}
