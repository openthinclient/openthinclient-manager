package org.openthinclient.service.common.license;

import org.springframework.context.ApplicationEvent;

public class LicenseChangeEvent extends ApplicationEvent {
  public LicenseChangeEvent(LicenseManager licenseManager) {
    super(licenseManager);
  }

  public LicenseManager getLicenseManager() {
    return (LicenseManager) getSource();
  }
}
