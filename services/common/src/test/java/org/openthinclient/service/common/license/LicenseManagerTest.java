package org.openthinclient.service.common.license;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.service.common.home.ManagerHome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {LicenseManagerInMemoryDatabaseConfiguration.class, LicenseManagerTestConfiguration.class })
public class LicenseManagerTest {

  @Autowired
  private ManagerHome managerHome;
  @Autowired
  private LicenseManager licenseManager;
  @Autowired
  private LicenseRepository licenseRepository;
  @Autowired
  private LicenseErrorRepository errorRepository;

  private ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  // taken from license-sever
  final String json = "{\"license\":\"4JOL6HRBeoOz6q6vvPlbQqbGWf+K6ukKolI+n7SioohNJYoHXljD1eX3JYx8JPoZ8q1o9KrUq/0riOrTZZKTKQ0AMlNx+ZEtclO1sIrDnz13zKVJ0OEaIyEoM76HoOLEgl3B8fPIj11Zh7Bp5Xv2Mxqgu2h4bQJh6rfQlsS6VjwJPU4311NnDLrC9L3UzIOhNIACLYC4jcReqVEBJnjWnZc53hLJOj5AdwDahy1jKmBUZiTX6MjHqh4baiIX9u2pZR69WYckjaIKFrW3XTusSC7yM7mij4V7ZTst0Oem1iZMV7Gb2ud2fUfnN9WHiMsS8vGc5X1SXndb09/q3aIlQfzAGUr0edALRXKj2fzdy7WV5mkxW1XHsC6NXD87iaa0tp0etaAWV+31+5Ueu+E8dDk6iuVL58kEVr7TDxyX+EleF3jverhEhPS0Vzw4sUQdNoqL7lskocy+D7GkhbM6hHqSBIc9sPUVVFuhe7mk73E2gIQ57eqa7UIjOAP3GZM9hYW6s1z0MwuFvPnx2lsb6P56dUUHNC5wGGIuY6AwwK6Ai/q0HcbtkD7Q2ETTr+KbvKawP9YyMjsy6w5J0oRnozCBeo/5pmLPGlXoregzMB7gvLO/EYTr/ZVMOpvJI1T8R5w1ZDBGkHUoPdbz0k0kig==\",\"encryption_key\":\"iPXo1BFzrPqPFlAZohm6QIDKZwIZh/jnNfMBvXwfycVl/kGrqBGRLlkbopuUtvKJSS10cXlEP5vKo+lRDCSiVhMQhwvF2jaVNPbp0BIZM1d8IR2u5mAQO3sG3vkHeoj/AfZUxd9zt5fIrDi678q53l9df3M0HLXLjDa7pcAS3ruoefJE2nvK4I3o3Qi0wTfTXlqE0RbOtapDjyr2a4Qq9vJJFTBg2vUCg43ob7sikjFKPvBHXyFbsKuEHKQXR1V3aW59HdFBX9zFZ0o6NNHb0QXd4lx79UnOUScNOIpWxxoLziazSZO7d5XIdtGE5vO+jPjNtDDqFrqhblm4q2wKkw==\"}";

  @Before
  public void before() {
    licenseManager.deleteLicense();
    licenseRepository.deleteAll();
    errorRepository.deleteAll();
  }

  @Test
  public void setLicenseTest() throws IOException {

    License license = licenseManager.getLicense();
    assertNull(license);

    EncryptedLicense encryptedLicense = mapper.readValue(json, EncryptedLicense.class);
    boolean success = licenseManager.setLicense(encryptedLicense);
    assertTrue(success);

    List<EncryptedLicense> licenseList = licenseRepository.findAll();
    assertNotNull(licenseList);
    assertEquals(1, licenseList.size());

    EncryptedLicense el = licenseList.get(0);
    assertEquals(encryptedLicense.license, el.license);

  }

  @Test
  public void licenseDetailsTest() throws IOException {

    License license = licenseManager.getLicense();
    assertNull(license);

    EncryptedLicense encryptedLicense = mapper.readValue(json, EncryptedLicense.class);
    boolean success = licenseManager.setLicense(encryptedLicense);
    assertTrue(success);

    License lic = licenseManager.getLicense();
    assertNotNull(lic);
    assertEquals(lic.server, managerHome.getMetadata().getServerID());
    assertEquals(Integer.valueOf(100), lic.getCount());

    assertEquals("Lizenz-Test 1 GmbH", lic.getName());
    assertEquals(null, lic.email);
    assertEquals("*Server-ID: ff00ff00-00e1-0000-0000-123123abcdef*\r\n\r\n||Bezeichnung||Restlaufzeit||Anzahl||\r\n|Kauflizenz Software und Support| | 100 St.||*Summe aller Lizenzen für diesen Standort (aktuell)*| |*100 St.*|", lic.getDetails());
    assertEquals("2020-01-18", lic.getSoftExpiredDate().toString());
    assertEquals("2020-02-18", lic.getExpiredDate().toString());
    assertEquals("2020-02-15", lic.getCreatedDate().toString());

  }

  @Test
  public void licenseStateTest() throws IOException {

    License license = licenseManager.getLicense();
    assertNull(license);

    EncryptedLicense encryptedLicense = mapper.readValue(json, EncryptedLicense.class);
    boolean success = licenseManager.setLicense(encryptedLicense);
    assertTrue(success);

    // Test: TODO: add testcases which make sense
    assertEquals(License.State.REQUIRED_TOO_OLD, licenseManager.getLicenseState(1));
    assertEquals(License.State.REQUIRED_TOO_OLD, licenseManager.getLicenseState(42));
    assertEquals(License.State.REQUIRED_TOO_OLD, licenseManager.getLicenseState(43));

  }

  @Test
  public void loadLicenseTwiceTest() throws IOException, InterruptedException {

    License license = licenseManager.getLicense();
    assertNull(license);

    EncryptedLicense encryptedLicense = mapper.readValue(json, EncryptedLicense.class);

    // first time
    boolean success = licenseManager.setLicense(encryptedLicense);
    assertTrue(success);

    Thread.sleep(1000);

    // second time
    success = licenseManager.setLicense(encryptedLicense);
    assertTrue(success);

    List<EncryptedLicense> licenseList = licenseRepository.findAll();
    assertNotNull(licenseList);
    assertEquals(1, licenseList.size());
  }
}
