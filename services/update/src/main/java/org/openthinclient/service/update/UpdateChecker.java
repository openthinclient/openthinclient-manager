package org.openthinclient.service.update;

import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.progress.NoopProgressReceiver;
import org.openthinclient.progress.ProgressReceiver;
import org.openthinclient.manager.util.http.DownloadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import java.net.URI;
import java.util.*;
import javax.annotation.PostConstruct;

public class UpdateChecker {

  private static final Logger LOGGER = LoggerFactory.getLogger(UpdateChecker.class);

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private DownloadManager downloadManager;

  @Value("${application.version}")
  private String applicationVersionString;
  @Value("${otc.application.version.update.location}")
  private String updateLocation;

  private AvailableVersionChecker versionChecker;
  private ProgressReceiver noopProgressReceiver = new NoopProgressReceiver();
  private Optional<String> newVersion = Optional.empty();
  private boolean hasNetworkError = false;

  private boolean isRunning = false;

  @PostConstruct
  public void init() {
    versionChecker = new AvailableVersionChecker(downloadManager);
  }

  public Optional<String> getNewVersion() {
    return newVersion;
  }

  public boolean isRunning() {
    return this.isRunning;
  }

  public boolean hasNetworkError() {
    return this.hasNetworkError;
  }

  public void fetchNewVersion() {
    isRunning = true;
    new Thread(() -> {
      boolean failure = false;
      try {
        UpdateDescriptor updateDescriptor = versionChecker.getVersion(new URI(updateLocation), noopProgressReceiver);
        String newVersionString = updateDescriptor.getNewVersion();
        if (com.install4j.api.update.UpdateChecker.isVersionGreaterThan(newVersionString, applicationVersionString)) {
          newVersion = Optional.of(newVersionString);
        } else {
          newVersion = Optional.empty();
        }
        this.hasNetworkError = false;
      } catch(DownloadException ex) {
        this.hasNetworkError = true;
        LOGGER.warn("Update check failed due to network error");
        failure = true;
      } catch (Exception ex) {
        this.hasNetworkError = false;
        LOGGER.error("Update check failed", ex);
        failure = true;
      }
      isRunning = false;
      applicationContext.publishEvent(new UpdateCheckerEvent(this, failure));
    }).start();
  }
}
