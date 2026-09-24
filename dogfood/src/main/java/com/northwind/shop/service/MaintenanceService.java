package com.northwind.shop.service;

import com.northwind.shop.report.ReportingDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Jobs the ops team triggers by hand. */
@Service
@Transactional
public class MaintenanceService {

  private final ReportingDao dao;

  public MaintenanceService(ReportingDao dao) {
    this.dao = dao;
  }

  public int renumberInvoices(String prefix) {
    return dao.renumberInvoices(prefix);
  }
}
