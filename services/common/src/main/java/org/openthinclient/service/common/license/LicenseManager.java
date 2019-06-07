package org.openthinclient.service.common.license;

import java.time.*;
import java.util.List;
import javax.annotation.PostConstruct;

import org.openthinclient.service.common.home.ManagerHome;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;


@Import({LicenseRepository.class})
public class LicenseManager {
    LicenseData license;
    LicenseDecrypter licenseDecrypter;
    int clientCount;
    String serverID;

    @Autowired
    LicenseRepository licenseRepository;

    @Autowired
    LicenseErrorRepository licenseErrorRepository;

    @Autowired
    ManagerHome managerHome;

    LicenseManager() {
    }

    @PostConstruct
    public void init() {
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

    private void decryptAndSetLicense(EncryptedLicense encryptedLicense) {
      try {
        this.license = licenseDecrypter.decrypt(encryptedLicense);
      } catch(Exception ex) {
        logError(LicenseError.ErrorType.DECRYPTION_ERROR);
      }
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

    public void setLicense(EncryptedLicense encryptedLicense) {
      saveEncryptedLicense(encryptedLicense);
      decryptAndSetLicense(encryptedLicense);
    }

    public LicenseData getLicense() {
      return this.license;
    }

    public LicenseData.State getLicenseState() {
      return LicenseData.getState(license, serverID, clientCount);
    }
}
