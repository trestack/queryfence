package com.acme.billing;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

  List<Invoice> findByTenantIdAndStatus(Long tenantId, String status);

  /** The bug this example is about: a derived query that forgets the tenant. */
  List<Invoice> findByStatus(String status);
}
