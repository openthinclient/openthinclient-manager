package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Printer;

public class DefaultLDAPPrinterService extends AbstractLDAPService<Printer> implements PrinterService {
  public DefaultLDAPPrinterService(RealmService realmService) {
    super(Printer.class, realmService);
  }
}
