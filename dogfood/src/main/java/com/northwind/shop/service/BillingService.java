package com.northwind.shop.service;

import com.northwind.shop.domain.Invoice;
import com.northwind.shop.domain.Payment;
import com.northwind.shop.report.ReportingDao;
import com.northwind.shop.repo.InvoiceRepository;
import com.northwind.shop.repo.PaymentRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BillingService {

  private final InvoiceRepository invoices;
  private final PaymentRepository payments;
  private final ReportingDao reporting;

  public BillingService(InvoiceRepository invoices, PaymentRepository payments,
      ReportingDao reporting) {
    this.invoices = invoices;
    this.payments = payments;
    this.reporting = reporting;
  }

  public List<Invoice> withStatus(long tenantId, String status) {
    return invoices.findByTenantIdAndStatus(tenantId, status);
  }

  public Page<Invoice> page(long tenantId, int page, int size) {
    return invoices.findByTenantId(tenantId, PageRequest.of(page, size));
  }

  public List<Invoice> unpaid(long tenantId) {
    return invoices.findUnpaid(tenantId);
  }

  public List<Object[]> statusTotals(long tenantId) {
    return invoices.statusTotals(tenantId);
  }

  public List<Payment> paymentsOf(long tenantId, long invoiceId) {
    return payments.findByTenantIdAndInvoiceId(tenantId, invoiceId);
  }

  public List<Object[]> paymentTotals(long tenantId) {
    return payments.totalsByMethod(tenantId);
  }

  public int markPaid(long tenantId, List<Long> invoiceIds) {
    return reporting.markInvoicesPaid(tenantId, invoiceIds);
  }

  /** Called from the payment webhook, which only knows the invoice number. */
  public Optional<Invoice> byNumber(String number) {
    return invoices.findByNumber(number);
  }

  /** The reconciliation screen lists every payment the instance recorded today. */
  public List<Payment> allPayments() {
    return payments.findAll();
  }
}
