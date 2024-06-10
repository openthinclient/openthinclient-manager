package org.openthinclient.api;

import org.openthinclient.common.Events.ClientCountChangeEvent;
import org.openthinclient.service.common.license.LicenseManager;
import org.openthinclient.service.store.LDAPConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsageStatusEndpoint {

  private static final Logger LOG = LoggerFactory.getLogger(UsageStatusEndpoint.class);

  @Autowired
  LicenseManager licenseManager;

  private String usageStatus = null;

  @GetMapping(value="/api/v2/usage-status",
              produces="text/plain;charset=utf-8")
  public String status() {
    if (usageStatus == null) {
      usageStatus = getUsageStatus();
    }
    return usageStatus;
  }

  @EventListener
  private void clientCountChanged(ClientCountChangeEvent event) {
    usageStatus = getUsageStatus();
  }

  private String getUsageStatus() {
    int clientCount;
    try (LDAPConnection ldapCon = new LDAPConnection()) {
      clientCount = ldapCon.countClients();
    } catch (Exception ex) {
      LOG.error("Could not count clients", ex);
      return "OK";
    }
    switch (licenseManager.getLicenseState(clientCount)) {
      case REQUIRED_TOO_OLD:  return "TOO_OLD";
      case REQUIRED_EXPIRED:  return "EXPIRED";
      case TOO_MANY:          return "TOO_MANY";
      case INVALID:           return "INVALID";
      case REQUIRED_MISSING:  return "MISSING";
      default:                return "OK";
    }
  }
}
