package org.openthinclient.service.update;

import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.pkgmgr.db.Version;
import org.openthinclient.progress.NoopProgressReceiver;
import org.openthinclient.progress.ProgressReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.util.*;
import javax.annotation.PostConstruct;

public class UpdateChecker {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpdateChecker.class);

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

  @PostConstruct
  public void init() {
    versionChecker = new AvailableVersionChecker(downloadManager);
    currentVersion = Version.parse(applicationVersionString);
  }

  public Optional<String> getNewVersion() {
    return newVersion;
  }

  public Optional<String> fetchNewVersion() throws java.net.URISyntaxException, javax.xml.bind.JAXBException, java.io.IOException {
    UpdateDescriptor updateDescriptor = versionChecker.getVersion(new URI(this.updateLocation), noopProgressReceiver);
    String newVersionString = updateDescriptor.getNewVersion();
    Version newVersion = Version.parse(newVersionString);
    int result = currentVersion.compareTo(newVersion);
    if (result < 0) {
      this.newVersion = Optional.of(newVersionString);
    } else {
      this.newVersion = Optional.empty();
    }
    return this.newVersion;
  }
}
