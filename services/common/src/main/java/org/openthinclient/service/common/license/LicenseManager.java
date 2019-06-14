package org.openthinclient.service.common.license;

import java.time.*;
import java.util.List;
import javax.annotation.PostConstruct;

import org.openthinclient.service.common.home.ManagerHome;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;


@Import({LicenseRepository.class})
public class LicenseManager {
    private LicenseData license;
    private String serverID;
    private LicenseDecrypter licenseDecrypter;

    @Autowired
    LicenseRepository licenseRepository;

    @Autowired
    LicenseErrorRepository licenseErrorRepository;

    @Autowired
    ManagerHome managerHome;

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

    private boolean decryptAndSetLicense(EncryptedLicense encryptedLicense) {
      LicenseData license;
      try {
        license = licenseDecrypter.decrypt(encryptedLicense);
      } catch(Exception ex) {
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

    public LicenseData getLicense() {
      return this.license;
    }

    public List<LicenseError> getErrors() {
      return licenseErrorRepository.findByOrderByDatetimeDesc();
    }

    public LicenseData.State getLicenseState(int clientCount) {
      return LicenseData.getState(license, serverID, clientCount);
    }
}
