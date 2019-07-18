package org.openthinclient.service.common.license;

import org.openthinclient.service.common.home.ManagerHome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;

@Import({LicenseRepository.class})
public class LicenseManager {

  private static final Logger LOG = LoggerFactory.getLogger(LicenseManager.class);

  private License license;
  private String serverID;
  private LicenseDecrypter licenseDecrypter;

  @Autowired
  LicenseRepository licenseRepository;

  @Autowired
  LicenseErrorRepository licenseErrorRepository;

  @Autowired
  ManagerHome managerHome;

  @Autowired
  ApplicationContext applicationContext;

  @PostConstruct
  public void init() {
    licenseErrorRepository.deleteByDatetimeBefore(LocalDateTime.now().minusMonths(1));
    serverID = managerHome.getMetadata().getServerID();
    try {
      licenseDecrypter = new LicenseDecrypter();
    } catch(Exception ex) {
    }
    loadLicense();
  }

  private void loadLicense() {
    List<EncryptedLicense> encryptedLicenses = licenseRepository.findAll();
    if(encryptedLicenses.size() > 0) {
      decryptAndSetLicense(encryptedLicenses.get(0), true);
    }
  }

  private boolean decryptAndSetLicense(EncryptedLicense encryptedLicense, boolean ignoreServerID) {
    License license;
    try {
      license = licenseDecrypter.decrypt(encryptedLicense);
    } catch(Exception ex) {
      LOG.error("Could not decrypt license", ex);
      logError(LicenseError.ErrorType.DECRYPTION_ERROR);
      return false;
    }
    if(!ignoreServerID && !this.serverID.equals(license.server)) {
      LOG.error("License not valid for this server");
      logError(LicenseError.ErrorType.SERVER_ID_ERROR);
      return false;
    }
    logError(LicenseError.ErrorType.UPDATED);
    this.license = license;
    applicationContext.publishEvent(new LicenseChangeEvent(this));
    return true;
  }

  private boolean decryptAndSetLicense(EncryptedLicense encryptedLicense) {
    return decryptAndSetLicense(encryptedLicense, false);
  }

  private void saveEncryptedLicense(EncryptedLicense encryptedLicense) {
    licenseRepository.deleteAll();
    licenseRepository.save(encryptedLicense);
  }

  void logError(LicenseError.ErrorType errorType, String details) {
    licenseErrorRepository.save(new LicenseError(errorType, details));
  }

  void logError(LicenseError.ErrorType errorType) {
    logError(errorType, null);
  }

  public void deleteLicense() {
    licenseRepository.deleteAll();
    this.license = null;
    applicationContext.publishEvent(new LicenseChangeEvent(this));
  }

  public boolean setLicense(EncryptedLicense encryptedLicense) {
    boolean success = decryptAndSetLicense(encryptedLicense);
    if(success) {
      saveEncryptedLicense(encryptedLicense);
    }
    return success;
  }

  public License getLicense() {
    return this.license;
  }

  public List<LicenseError> getErrors() {
    return licenseErrorRepository.findTop25ByOrderByDatetimeDesc();
  }

  public License.State getLicenseState(int clientCount) {
    return License.getState(license, serverID, clientCount);
  }
}
