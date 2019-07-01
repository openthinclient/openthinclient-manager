package org.openthinclient.api.rest.impl;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import org.openthinclient.api.i18n.RestMessages;
import org.openthinclient.api.rest.model.License;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.service.common.license.LicenseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.stream.Stream;

import static org.openthinclient.api.i18n.RestMessages.*;

@RestController
@RequestMapping("/api/v1/licensing")
public class LicenseResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(LicenseResource.class);

  private final ClientService clientService;
  private final LicenseManager licenseManager;
  private IMessageConveyor mcEN = new MessageConveyor(Locale.ENGLISH);
  private IMessageConveyor mcDE = new MessageConveyor(Locale.GERMAN);

  public LicenseResource(ClientService clientService, LicenseManager licenseManager) {
    this.clientService = clientService;
    this.licenseManager = licenseManager;
  }

  /**
   * zugeordnet werden:
   * License.State 	i18n-Key aus SOFTWARE-952
   * REQUIRED_TOO_OLD 	REST_LICENSE_THINCLIENT_COMMUNICATION_ERROR
   * REQUIRED_OLD
   * REQUIRED_EXPIRED 	REST_LICENSE_THINCLIENT_LICENSE_EXPIRED
   * SOFT_EXPIRED
   * OK
   * INVALID 	REST_LICENSE_THINCLIENT_CRITICAL_ERROR
   * REQUIRED_MISSING 	REST_LICENSE_THINCLIENT_BUY_LICENSE
   * TOO_OLD
   * OLD
   * EXPIRED
   *
   * @return
   */
  @GetMapping
  public License getLicense() {

    int clients = clientService.findAll().size();
    org.openthinclient.service.common.license.License.State licenseState = licenseManager.getLicenseState(clients);

    switch (licenseState) {
      case REQUIRED_TOO_OLD: return new License(3, getMessages(REST_LICENSE_THINCLIENT_COMMUNICATION_ERROR));
      case REQUIRED_EXPIRED: return new License(3, getMessages(REST_LICENSE_THINCLIENT_LICENSE_EXPIRED));
      case INVALID:          return new License(3, getMessages(REST_LICENSE_THINCLIENT_CRITICAL_ERROR));
      case REQUIRED_MISSING: return new License(3, getMessages(REST_LICENSE_THINCLIENT_BUY_LICENSE));
      default: return new License();
    }

  }

  private String[] getMessages(RestMessages key) {
    return Stream.of(mcDE.getMessage(key), mcEN.getMessage(key)).toArray(String[]::new);
  }

}
