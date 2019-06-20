package org.openthinclient.api.rest.impl;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.license.License;
import org.openthinclient.service.common.license.LicenseManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LicenseRepositoryTest {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();
  @Mock
  ClientService clientService;
  @Mock
  LicenseManager licenseManager;
  @Mock
  ManagerHome managerHome;

  @Test
  public void testLicenseOK() {

    Mockito.when(clientService.findAll()).thenReturn(java.util.Collections.singleton(new Client()));
    Mockito.when(licenseManager.getLicenseState(1)).thenReturn(License.State.OK);

    final LicenseResource lr = new LicenseResource(clientService, licenseManager);
    org.openthinclient.api.rest.model.License license = lr.getLicense();
    assertEquals("Wrong interval", 0, license.getPopupInterval());
    assertEquals("Empty message expected", "", license.getPopupTextDe());
    assertEquals("Empty message expected", "", license.getPopupTextEn());
  }

  @Test
  public void testLicenseREQUIRED_TOO_OLD() {

    Mockito.when(clientService.findAll()).thenReturn(java.util.Collections.singleton(new Client()));
    Mockito.when(licenseManager.getLicenseState(1)).thenReturn(License.State.REQUIRED_TOO_OLD);

    final LicenseResource lr = new LicenseResource(clientService, licenseManager);
    org.openthinclient.api.rest.model.License license = lr.getLicense();
    assertEquals("Wrong interval", 3, license.getPopupInterval());
    assertTrue("Message expected", license.getPopupTextDe().length() > 0);
    assertTrue("Message expected", license.getPopupTextEn().length() > 0);
  }

}