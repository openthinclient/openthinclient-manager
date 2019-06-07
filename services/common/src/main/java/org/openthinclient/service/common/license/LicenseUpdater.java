package org.openthinclient.service.common.license;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.HttpURLConnection;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;


public class LicenseUpdater {
  private static final Logger LOG = LoggerFactory.getLogger(LicenseUpdater.class);

  private static final String LICENSE_REST_URL = "https://support.openthinclient.com/openthinclient/rest/scriptrunner/latest/custom/get_license/";

  @Autowired
  LicenseManager licenseManager;

  private ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  void updateLicense(String serverID) {
    LOG.info("Updating license information.");

    URL url;
    try {
      url = new URL(LICENSE_REST_URL + serverID);
    } catch(java.net.MalformedURLException ex) {
      LOG.error("Failed to build license URL", ex);
      return;
    }

    try {
      HttpURLConnection con = (HttpURLConnection)url.openConnection();
      con.setRequestProperty("Content-Type", "application/json");
      con.setRequestProperty("charset", "utf-8");
      int responseCode = con.getResponseCode();
      if(responseCode == HttpURLConnection.HTTP_OK) {
        EncryptedLicense encryptedLicense = mapper.readValue(con.getInputStream(), EncryptedLicense.class);
        if(encryptedLicense.license == null) {
          LOG.info("No license available.");
          licenseManager.logError(LicenseError.ErrorType.NO_LICENSE);
        } else {
          LOG.info("New license information received.");
          licenseManager.setLicense(encryptedLicense);
        }
      } else {
        LOG.info("Failed to update license information. Server responded with "+ responseCode);
        licenseManager.logError(LicenseError.ErrorType.SERVER_ERROR);
      }
    } catch(java.io.IOException ex) {
      LOG.error("Failed to get license from " + url, ex);
      licenseManager.logError(LicenseError.ErrorType.NETWORK_ERROR);
    }
  }
}
