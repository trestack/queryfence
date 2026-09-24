package com.northwind.shop.repo;

import com.northwind.shop.domain.PurchaseOrder;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Criteria building blocks for the order search screen. */
public final class PurchaseOrderSpecifications {

  private PurchaseOrderSpecifications() {}

  public static Specification<PurchaseOrder> ofTenant(Long tenantId) {
    return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
  }

  public static Specification<PurchaseOrder> withStatus(String status) {
    return (root, query, cb) -> cb.equal(root.get("status"), status);
  }

  public static Specification<PurchaseOrder> totalAbove(BigDecimal floor) {
    return (root, query, cb) -> cb.greaterThan(root.get("total"), floor);
  }

  public static Specification<PurchaseOrder> notDeleted() {
    return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
  }

  /** Free-form search used by the support console: whatever the operator filled in. */
  public static Specification<PurchaseOrder> freeSearch(String status, Long customerId) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (status != null) {
        predicates.add(cb.equal(root.get("status"), status));
      }
      if (customerId != null) {
        predicates.add(cb.equal(root.get("customerId"), customerId));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
