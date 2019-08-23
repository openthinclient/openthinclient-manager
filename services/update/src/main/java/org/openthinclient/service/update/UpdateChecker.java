package org.openthinclient.service.update;

import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.pkgmgr.db.Version;
import org.openthinclient.progress.NoopProgressReceiver;
import org.openthinclient.progress.ProgressReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import java.net.URI;
import java.util.*;
import javax.annotation.PostConstruct;

public class UpdateChecker {

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
  private Version currentVersion;
  private Optional<String> newVersion = Optional.empty();

  private boolean isRunning = false;

  @PostConstruct
  public void init() {
    versionChecker = new AvailableVersionChecker(downloadManager);
    currentVersion = Version.parse(applicationVersionString);
  }

  public Optional<String> getNewVersion() {
    return newVersion;
  }

  public boolean isRunning() {
    return this.isRunning;
  }

  public void fetchNewVersion() {
    isRunning = true;
    new Thread(() -> {
      boolean failure = false;
      try {
        UpdateDescriptor updateDescriptor = versionChecker.getVersion(new URI(updateLocation), noopProgressReceiver);
        String newVersionString = updateDescriptor.getNewVersion();
        int result = currentVersion.compareTo(Version.parse(newVersionString));
        if (result < 0) {
          newVersion = Optional.of(newVersionString);
        } else {
          newVersion = Optional.empty();
        }
      } catch (Exception ex) {
        failure = true;
      }
      isRunning = false;
      applicationContext.publishEvent(new UpdateCheckerEvent(this, failure));
    }).start();
  }
}
