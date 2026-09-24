package com.northwind.shop.repo;

import com.northwind.shop.domain.Customer;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

  List<Customer> findByTenantIdAndStatus(Long tenantId, String status);

  Optional<Customer> findByTenantIdAndEmail(Long tenantId, String email);

  Page<Customer> findByTenantIdAndDeletedAtIsNull(Long tenantId, Pageable pageable);

  long countByTenantIdAndStatus(Long tenantId, String status);

  /** Used by the back-office customer list; the tenant comes from the request context. */
  @Query("select c from Customer c where c.tenantId = :tenantId and c.deletedAt is null"
      + " order by c.name asc")
  List<Customer> listActive(@Param("tenantId") Long tenantId);

  @Query(value = "select * from customer where tenant_id = :tenantId"
      + " and lower(name) like lower(concat('%', :term, '%')) and deleted_at is null",
      nativeQuery = true)
  List<Customer> searchByName(@Param("tenantId") Long tenantId, @Param("term") String term);

  @Modifying
  @Query("update Customer c set c.deletedAt = :now where c.id = :id and c.tenantId = :tenantId")
  int softDelete(@Param("tenantId") Long tenantId, @Param("id") Long id,
      @Param("now") Instant now);

  /**
   * Nightly tidy-up of customers a tenant closed. Written before the service became multi-tenant
   * and never revisited.
   */
  @Modifying
  @Query("update Customer c set c.status = 'ARCHIVED' where c.deletedAt is not null")
  int archiveDeleted();

  // The support console looks customers up by e-mail across the whole instance.
  List<Customer> findByStatusOrderByNameAsc(String status);
}
