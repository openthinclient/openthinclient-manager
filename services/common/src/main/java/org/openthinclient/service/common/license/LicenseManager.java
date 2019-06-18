package org.openthinclient.service.common.license;

import java.time.LocalDateTime;
import java.util.List;
import javax.annotation.PostConstruct;

import org.openthinclient.service.common.home.ManagerHome;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;


@Import({LicenseRepository.class})
public class LicenseManager {
    private License license;
    private String serverID;
    private LicenseDecrypter licenseDecrypter;
    private static final Logger LOG = LoggerFactory.getLogger(LicenseUpdater.class);

    @Autowired
    LicenseRepository licenseRepository;

    @Autowired
    LicenseErrorRepository licenseErrorRepository;

    @Autowired
    ManagerHome managerHome;

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
        decryptAndSetLicense(encryptedLicenses.get(0));
      }
    }

    private boolean decryptAndSetLicense(EncryptedLicense encryptedLicense) {
      License license;
      try {
        license = licenseDecrypter.decrypt(encryptedLicense);
      } catch(Exception ex) {
        LOG.error("Could not decrypt license", ex);
        logError(LicenseError.ErrorType.DECRYPTION_ERROR);
        return false;
      }
      if(!this.serverID.equals(license.server)) {
        logError(LicenseError.ErrorType.SERVER_ID_ERROR);
        return false;
      }
      logError(LicenseError.ErrorType.UPDATED);
      this.license = license;
      return true;
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
      return licenseErrorRepository.findByOrderByDatetimeDesc();
    }

    public License.State getLicenseState(int clientCount) {
      return License.getState(license, serverID, clientCount);
    }
}
