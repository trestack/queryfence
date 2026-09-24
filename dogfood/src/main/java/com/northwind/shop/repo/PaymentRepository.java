package com.northwind.shop.repo;

import com.northwind.shop.domain.Payment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

  List<Payment> findByTenantIdAndInvoiceId(Long tenantId, Long invoiceId);

  @Query("select p.method, sum(p.amount) from Payment p where p.tenantId = :tenantId"
      + " group by p.method")
  List<Object[]> totalsByMethod(@Param("tenantId") Long tenantId);
}
