package com.acme.billing;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InvoiceService {

  private final InvoiceRepository invoices;

  public InvoiceService(InvoiceRepository invoices) {
    this.invoices = invoices;
  }

  public List<Invoice> openInvoicesOf(long tenantId) {
    return invoices.findByTenantIdAndStatus(tenantId, "OPEN");
  }

  public List<Invoice> openInvoices() {
    return invoices.findByStatus("OPEN");
  }
}
