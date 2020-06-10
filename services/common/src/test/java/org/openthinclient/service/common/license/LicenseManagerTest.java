package org.openthinclient.service.common.license;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
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
  final String json = "{\"license\":\"/dOBTApfd5RJop5ux53ltUHo5Tpihx7wVAEKEHedRmZFggI2TOEcU9gH8uvHuaM3mUSpyP2OkAmV26uKTtkAY0yKNtsr2hE/byv7gS7pHUGQ8LTJBXa20xnnd7YpnCKbZrgSpLVFE33oZGIdeeT9eyFtcK9OH1cQ/JNfGllTliC57D0FPKmqNE0sr2tze8R0Qyv3SzY9j+sFcIPEJO5MErXsJtlmnlyvdhqZ8VWCf/kuT6MEE7NKXKgrcL3Tp9YYyzyBMHibMU8EI4RazishApCYA7kf20h0ySzYJtLR1M9dbiSSByhN67YP6KalHBYieMvJ+MEuvo/qeISYxbCdpCugk8nQYPsFhae8CiRAsts37eYVmOsli74O6Ja1xcYcGlxRer2rVAQu/K8E31V1toaK3QGOpQe7+G6ICCGCfh3/qla4NDrYlFYZ1ZLkw+kOyxuJ2DOOIIO7RgmOal0SiW9RcDg0DHo2SOsND1hNw0jOhMhkftBAbYY/5sS2i93lnpQZPmPvzvl/5qW78QtFTxzqOhooP5TfFol/CfxdLmhZno7eMaWxiakK8WyWoNyuDAre/4iVBx0vm6I4Irn1RCV77p+R49jHZvEzbdj2N71ltgUhts+HuTTyku6D/GmPq1jGXQ8vXFnEK2O7v2xUkWC2AgohAs7Dnnos9Nh0mLoDi03tFflCX/uPEAnAlJfPYK91SwlgG2nDH+wdMfH5ykKGu8wdkBgOy3HyW3XId1uS4fKPKzvY/eM7l8R/cKSrMB4Gzw2AapnN5LF6YeS6xiLng+QMY4R4PG9ALsOwVSVowQ6ZDzzb3cTnYsZ8HBBN2U+7RWvl9t9vkJuF60nojqQT4AuqB9EHiJzvm8i4zUAhpokgELiyyKYCqfazc2e2yJl8GGtem1OgSJHqMh+dyvsZEAoWTynhoYBRCTO2HOoWamhGHfCIaWm/x9EDMIqVLjJRAb1MapaEXFnBYWC/crAluw7qhsRGvvz3pp/iLnMURN4Ii8rrr9qBTx6zrEK9xGSDufMRCcLSCXkaS5p8IqL0sBmysQtHoX6U0ZCv0jK9zdY6iGHwG2geoNZ2EMxsnecX5h8PqOM5636BDkVK1xzqOhooP5TfFol/CfxdLmhZno7eMaWxiakK8WyWoNyuJp2evDAXhcK3fXsEn3JajyV77p+R49jHZvEzbdj2N71ltgUhts+HuTTyku6D/GmPexnnki3xW9jjh6mVRbgTYWC2AgohAs7Dnnos9Nh0mLoDi03tFflCX/uPEAnAlJfP792DjIQ9fL4W/WPXQFxfa7/1yw7pbiBsKfEV+7zaFBQdSXifqW2eO/uBr47SPugxbgKSVJUNbhWRhT4eLQq/1ZyXd3aMheJ234IYf2497wi0GOM16DR2iV1nNmZP/wtTu8VZkZd5AegNg6+ejeFbGA==\",\"encryption_key\":\"VRRf+ZTE/r/vvRTNsJA8PqlK+FWofoWQd+qUeXcvNi/2sZsvLK68pfJ5/UbiF70YemPfKllV+ID7cUiLX4KgBvAryGVK9fi+i0d2n4VKZEQEHWdspzJ/DWtxPTrfNHJ47A7ssK11DNDH7Na/XIKP84pba2AcVfMSIJOb14OW9GGxWvlxxgNHQITWT5FI24AdtCnBn+/YzDkFMQ3QCewK5Unr478hNozyTUZC2aXpVWJhQtueu6qNv7oIPIFFGbjJXZifmDQJDB895lHSQTS8Sm3/WykOLrDRkBJX+qkbLse++vuDxrYgHsZmx0Bbu5OpAoDXWssuAQyQw8fHhDMp1g==\"}";

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
    licenseManager.setLicense(encryptedLicense);

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
    licenseManager.setLicense(encryptedLicense);

    // Test
    License lic = licenseManager.getLicense();
    assertNotNull(lic);
    assertEquals(lic.server, managerHome.getMetadata().getServerID());
    assertEquals(Integer.valueOf(42), lic.getCount());

    assertEquals("Duravit AG", lic.getName());
    assertEquals("sybille.robel@duravit.de      ", lic.email);
    assertEquals("*Server-ID: 0815-777-12345*\r\n\r\n||Bezeichnung||Restlaufzeit||Anzahl||\r\n|Kauflizenz Software und Support| |  10 St.||Inklusivlizenz bei Hardwarekauf|23 Monate|   1 St.|\r\n|Inklusivlizenz bei Hardwarekauf|19 Monate|   1 St.|\r\n|Inklusivlizenz bei Hardwarekauf|17 Monate|   3 St.|\r\n|Inklusivlizenz bei Hardwarekauf|15 Monate|  11 St.|\r\n|Inklusivlizenz bei Hardwarekauf|14 Monate|   1 St.|\r\n|Inklusivlizenz bei Hardwarekauf|13 Monate|   5 St.|\r\n|Inklusivlizenz bei Hardwarekauf|10 Monate|   2 St.|\r\n|Inklusivlizenz bei Hardwarekauf| 9 Monate|   5 St.|\r\n|Inklusivlizenz bei Hardwarekauf| 7 Monate|   1 St.|\r\n|Inklusivlizenz bei Hardwarekauf| 5 Monate|   1 St.|\r\n|Inklusivlizenz bei Hardwarekauf| 4 Monate|   1 St.|\r\n|*Summe aller Lizenzen für diesen Standort (aktuell)*| |*42 St.*|", lic.getDetails());
    assertEquals("2019-07-01", lic.getSoftExpiredDate().toString());
    assertEquals("2019-08-01", lic.getExpiredDate().toString());
    assertEquals("2019-06-12", lic.getCreatedDate().toString());

  }

  @Test
  public void licenseStateTest() throws IOException {

    License license = licenseManager.getLicense();
    assertNull(license);

    EncryptedLicense encryptedLicense = mapper.readValue(json, EncryptedLicense.class);
    licenseManager.setLicense(encryptedLicense);

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
    licenseManager.setLicense(encryptedLicense); // first

    Thread.sleep(1000);

    licenseManager.setLicense(encryptedLicense); // second

    List<EncryptedLicense> licenseList = licenseRepository.findAll();
    assertNotNull(licenseList);
    assertEquals(1, licenseList.size());
  }
}