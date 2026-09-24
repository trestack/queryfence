package com.northwind.shop.repo;

import com.northwind.shop.domain.Invoice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

  List<Invoice> findByTenantIdAndStatus(Long tenantId, String status);

  Page<Invoice> findByTenantId(Long tenantId, Pageable pageable);

  @Query("select i from Invoice i where i.tenantId = :tenantId"
      + " and not exists (select 1 from Payment p where p.invoiceId = i.id"
      + "   and p.tenantId = i.tenantId)")
  List<Invoice> findUnpaid(@Param("tenantId") Long tenantId);

  @Query(value = "select i.status, count(*), sum(i.amount) from invoice i"
      + " left join payment p on p.invoice_id = i.id and p.tenant_id = i.tenant_id"
      + " where i.tenant_id = :tenantId group by i.status having count(*) >= 1",
      nativeQuery = true)
  List<Object[]> statusTotals(@Param("tenantId") Long tenantId);

  // Invoice numbers are globally unique, so the tenant seemed unnecessary here.
  Optional<Invoice> findByNumber(String number);
}
