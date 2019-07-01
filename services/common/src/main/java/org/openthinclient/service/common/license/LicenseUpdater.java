package org.openthinclient.service.common.license;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URISyntaxException;

import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.progress.NoopProgressReceiver;
import org.openthinclient.progress.ProgressReceiver;
import org.openthinclient.manager.util.http.DownloadException;
import org.openthinclient.manager.util.http.StatusCodeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;


public class LicenseUpdater {
  private static final Logger LOG = LoggerFactory.getLogger(LicenseUpdater.class);

  private static final String LICENSE_REST_URL = "https://support.openthinclient.com/openthinclient/rest/scriptrunner/latest/custom/get_license/";

  private final static ProgressReceiver noopProgressReceiver = new NoopProgressReceiver();

  @Autowired
  LicenseManager licenseManager;

  @Autowired
  private DownloadManager downloadManager;

  private ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public void updateLicense(String serverID) {
    LOG.info("Updating license information.");

    URI uri;
    try {
      uri = new URI(LICENSE_REST_URL + serverID);
    } catch(URISyntaxException ex) {
      LOG.error("Failed to build license URL", ex);
      return;
    }

    try {
      EncryptedLicense encryptedLicense = downloadManager.download(uri, in -> {
        return mapper.readValue(in, EncryptedLicense.class);
      }, noopProgressReceiver);
      if(encryptedLicense.license == null) {
        LOG.info("No license available.");
        licenseManager.logError(LicenseError.ErrorType.NO_LICENSE);
      } else {
        LOG.info("New license information received.");
        licenseManager.setLicense(encryptedLicense);
      }
    } catch(StatusCodeException ex) {
      LOG.info("Failed to update license information. Server responded with "+ ex.getStatusCode() + " " + ex.getReasonPhrase());
      licenseManager.logError(LicenseError.ErrorType.SERVER_ERROR);
    } catch(DownloadException ex) {
      LOG.error("Failed to get license from " + uri, ex);
      licenseManager.logError(LicenseError.ErrorType.NETWORK_ERROR);
    }
  }
}
